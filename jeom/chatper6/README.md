# 6장 AOP

스프링이 제공하는 깔끔한 트랜잭션 인터페이스를 썼음에도 비즈니스 로직이 주인이어야할 메소드안에 이름도 길고 무시무시하게 생긴 트랜잭션 코드가 더 많은 자리를 차지 하고 있다 .

비즈니스 로직과 트랜잭션 경계설정 코드는 성격이 다를 뿐만아니라 서로 주고받는것도 없는 완벽하게 독립적인코드다 . 

다만 이 비즈니스 로직을 담당하는 코드가 트랜잭션의 시작과 종료 작업사이에서 수행돼야 한다는 사항만 지켜지면된다 . 그렇다면 이 성격이다른 코드를 두개의 메소드로 분리 할수 있지 않을까 ?

> 비즈니스 로직과 트랜잭션 경계 설정의 분리 

```java
public void upgradeLevel() throws Exception{
	TrancactionStatus status = this,transactionManager
		.getTransaction(new DefaultTransactionDefinition());
	try{
		upgradeLevelsInternal();
		this.transactionManger.commit(status);
	}catch(Exception e){
		this.transactionManager.rollback(status);
		throw e;
	}
	
	private void upgradeLevelsInternal(){ //분리된 비즈니스 로직코드 
		List<User> users = userDao.getAll();
		for (User user : users){
			if(canUpgradeLevel(user)){
				urgradeLevel(user);
			}
		}
	}
}
```

여전히 트랜잭션을 담당하는 기술적인 코드가 버젓이 UserService 안에 자리 잡고 있다 . 간단하게 트랜잭션 코드를 클래스 밖으로 뽑아내자.

### DI적용을 이용한 트랜잭션 분리

트랜잭션 코드를 UserService 밖으로 분리해버리면 UserService를 직접 사용하는 클라이언트 코드에서는 트랜잭션 기능이 빠진 UserService를 사용하게 될것이다 . 

직접적으로 사용하는 것이 문제가 된다면 간접적으로 사용하면 된다 . DI의 기본 아이디어는 실제 사용할 오브젝트의 클래스의 정체는 감춘채 인터페이스를 통해 간접으로 접근하는것이다 . 그덕분에 구현 클래스는 얼마든지 외부에서 변경할 수 있다 .이런 개념을 가진 DI가 지금 필요하다.

UserService 를 인터페이스로 만들고 기존코드는 UserService 인터페이스의 구현 클래스를 만들어 넣도록 한다 .

UserService 를 구현한 또 다른 구현 클래스를 만든다 . 단지 트랜잭션의 경계 설정이라는 책임을 담당하고 있을 뿐이다 . 

스스로는 비즈니스 로직을 담고 있지 않기 때문에 또 다른 비즈니스 로직을 담고 있는 UserService구현 클래스에 실제적인 로직 처리 작업을 위임한다.

> UserService 인터페이스 도입 

```java
public interface UserService{
	void add(User user);
	void uparadeLevels();
}
```

> 트랜잭션 코드를 제거한 UserService

```java
public class UserServiceImpl implements UserService{
	UserDao userDao;
	
	public void upgradeLevels(){
		List<User> users = userDao.getAll();
		for(User user : users){
			if(canUpgradeLevel(user)){
				upgradeLevel(user);
			}
		}
	}
	...
}
```

> 트랜잭션 처리를 담은 UserServiceTx

```java
public class UserServiceTx implements UserService{
	UserService userService;
    PlatefromTransactionManager transavtionManager;
    
    pulbic void setTransactionManager(PlatfromTransactionManager transactionManager){
        this.transactionManager = transactionManager;
    }
	
	public void setUserService(UserService userService){
		this.userService = userService;
	}
	//UserService 를 구현한 다른 오브젝트를 DI받음
	
	public void ass(user user){
		userService.add(user);
	}
	public void upgradeLevels(){
        TrancationStatus suatus = this.transactionManager.getTrancaction(new DefaultTransactionDefinition());
        try{
            userService.upgradeLevels();
            this.transactionManager.commit(status);
        }catch(RuntimeException e){
            this.transactionManager.rollback(satatus);
            throw e;
        }
	
	}
	//다른 기능을 UserService 오브젝트에게 위임 
}
```

> 설정파일수정

```xml
<bean id="userService" class="springbook.user.service.UserServiceTx"> 
		<property name="transactionManager" ref="transactionManager" /> 
		<property name=“ userService" ref=“ userServicelmpl" /> 
</bean> 
```

### 고립된 단위 테스트 

테스트 대상이 테스트 단위인 것 처럼 보이지만 사실은 그뒤의 의존 관계를 따라 등장하는 오브젝트와 서비스 , 환경등이 모두 합쳐서 테스트 대상이 된다 . 이런 경우 환경이 조금이라도 달라지면 동일한 테스트 결과를 내지 못 할 수도 있다. 

그래서 테스트의 대상이 환경이나 , 외부서버, 다른 클래스의 코드에 종속 되고 영향을 받지 않도록 고립시킬 필요가 있다 . 테스트를 대상으로부터 분리해서 고립시키는 방법은 테스트를 위한 대역을 사용하는것이다.

>  UserDao  오브젝트

```java
static class MockUserDao implement UserDao{
	private List<User> users; //레벨 업그레이드 후보 
	private List<User> updated = new ArrayList(); //업그레이드 대상 오브젝트 
	
	private MockUserDao(List<User> users){
		this.users = users;
	}
	public List<User> getUpdated(){
		return this.updated;
	}
	public List<User> getAll(){ //스텁기능 제공
		return this.users;
	}
	public void update(User user){ //목 오브젝트 기능 제공
		updated.add(user);
	}
	
	public void add(User user){ throw new UnsupportedOperationException(); }
	...
	//테스트에 사용되지 않는 메소드는 실수로 사용될 위험이 있으므로 에러를 던지게 해준다.
}
```

> MockUserDao를 사용하는 고립테스트

```java
public void upgradeLevels() throws Exception{

	UserServiceImpl userServiceImpl = new UserServiceImpl();
	
	MockUserDao mockUserDao = new MockUserDao(this.users);
	userServiceImpl.setUserDao(mockUserDao);
	
	userServiceImpl.upgradeLevels();
	
	List<User> updated = mockUserDao.getUpdated();
	assrtThat(updated.size(),is(2));
	checkUserAndLevel(updated.get(0),"joytouch",Level.SILVER);
	checkUserAndLevel(updated.get(1),"madnite1",Level.GOLD);
	
}

private void checkUserAndLevel(User updated , String expectedId , Level expectedLevel){
	assertThat(updated.getId(),is(expetedId));
	assertThat(updated.getLevel(),is(expetedLevel);
}
```

