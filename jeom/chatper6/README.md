# 6장 

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

## 프록시

부가기능이 마치 자신이 핵심기능을 가진 클래스 인것 처럼 꾸며서 , 클라언트가 자신을 거쳐서 핵심기능을 사용하도록 만들어야한다.

그러기 위해서는 클라이언트는 인터페이스를 인터페이스를 통해서만 핵심 기능을 사용하게 해야하고 , 부가기능 자신도 같은 인터페이스를 구현한 뒤에 자신이 그사이에 끼어들어야한다.

그러면 클라이언트는 인터페이스만 보고 사용을 하기 때문에 자신은 핵심기능을 가진 클래스를 사용 할 것이라고 기대하지만. 사실은 부가기능을 통해 핵심기능을 이용하게 되는것이다.

**이렇게 마치 자신이 클라이언트가 사용하려고하는 실제 대상인것 처럼 위장해서 클라이언트의 요청을 받아주는것을 대리자, 대리인과 같은 역할을 한다고 해서 프록시라고 부른다.**

프록시의 특징은 타깃과 같은 인터페이스를 구현 했다는 것과 프록시가 타깃을 제어할수 있는 위치에 있다는것이다 . 프록시는 사용목적에 따라 두가지로 구분할수있다 . 첫째는 클라이언트가 타깃에게 접근하는 방법을 제어하기 위해서다. 두번째는 타깃에 부가적인 기능을 부여해주기 위해서다.

## 데코레이터 패턴 

타깃에 부가적인 기능을 런타임 시 다이내믹하게 부여해주기 위해 프록시를 사용하는 패턴을 말한다.

다이내믹하게 기능을 부가한다는 의미는 컴파일 시점 , 즉 코드상에서는 어떤 방법과 순서로 프록시와 타깃이 연결되어 사용되는지 정해져 있지않다. 

데코레이터 패턴에서는 프록시가 꼭 한개로 제한되지 않는다 . 프록시가 직접 타깃을 사용하도록 고정시킬 필요 도 없다.

이를 위해 데코레이터 패턴에서는 같은 인터페이스를 구현한 타겟과 여러개의 프록시를 사용 할수 있다 . 프록시가 여러개인만큼 순서를 정해서 단계적으로 위임하는 구조로 만들면 된다.

데코레이터의 다음 위임 대상은 인터페이스로 선언하고 생성자나 수정자 메소드를 통해 위임 대상을 외부에서 란타임 시에 주입 받을수 있도록 만들어야한다.

다음 코드는 InputStream이라는 인터페이스를 구현한 타깃인 FileInputStream에 버퍼 읽기를 제공해주는 BufferdInputStream 이라는 데코레이터를 적용한 예다.

`InputStream is = new BufferdInputStream(new FileInputStream("a.txt"));` 

위에 UserServiceTx 클래스로선언된 userService 빈은 데코레이터다.

```xml
<!-- 데코레이터 -->
<bean id ="userService" class="springbook.user.service.UserServiceTx">
	<property name ="transactionManager" ref="transactionManager"/>
	<property name ="userService" ref="userServiceImpl"/>
</bean>
<!-- 타깃 -->
<bean id ="userServiceImpl" class="springbook.user.service.UserServiceImpl">
	<property name="userDao" ref="userDao"/>
	<property name="mailSender" ref="mailSender"/>
</bean>
```

## 프록시 패턴

일반적으로 사용하는 프록시 라는 용어와 디자인 패턴에서 말하는 프록시 패턴은 구분 할 필요가 있다 . 

전자는 클라이언트와 사용 대상 사이에 대리 역할을 맡은 오브젝트를 두는 방법을 총칭한다면, 후자는 프록시를 사용 하는 방법중에서 타깃에 대한 접근 방법을 제어하려는 목적을 가진 경우를 가리킨다.

프록시 패턴의 프록시는 타깃의 기능을 확장하거나 추가하지 않는다. 대신 클라이언트가 타깃에게 접근하는 방식을 변경해준다 . 타깃오브젝트를 생성하기가 복잡하거나 당장 필요하지 않은 경우에는 꼭 필요한 시점까지 오브젝트를 생성하지 않는 편이 좋다.  

그런데 타깃 오브젝트에 대한 레퍼런스가 미리 필요할수도있다 . 이럴때 프록시 패턴을 적용하면된다. 클라이언트에게 타깃에대한 레퍼런스를 넘겨야하는데 , 실제 타깃 오브젝트는 만드는 대신 프록시를 넘겨주는 것이다.

원격 오브젝트를 이용하는 경우에도 원격 오브젝트에 대한 프록시를 만들어주고 , 클라이언트는 마치 로컬에 존재하는 오브젝트를 스는것처럼 프록시를 사용하게 할수 있다.

또는 특별한 상황에서 타깃에대한 접근권한을 제어하기 위해 프록시 패턴을 사용할수있다.  만약 수정 가능한 오브젝트가 있는데 , 특정레이어로 넘어가서는 읽기 전용으로만 동작하게 강제 해야한다고하자 . 이럴때는 오브젝트의 프록시를 만들어서 사용할수있다 . 프록시의 특정 메소드를 사용하려고 하면 접근이 불가능 하다고 예외를 발생시키면된다.

이렇게 프록시 패턴은 타깃의 기능 자체에는 관여하지 않으면서 접근하는 방법을 제어 해주는 프록시를 이용하는 것이다. 구조적으로 보자면 프록시와 데코레이터는 유사하다 . 다만 프록시는 코드에서 자신이 만들거나 접근할 타깃 클래스 정보를 알고있는 경우가 많다. 생성을 지연하는 프록시 라면 구체적인 생성 방법을 알아야 하기 때문에 타깃 클래스에 대한 직접 적인 정보를 알고 있어야한다 . 

물론 프록시 패턴이라고 하더라도 인터페이스를 통해 위임하도록 만들 수도 있다 . 인터페이스를 통해 다음 호출 대상으로 접근하게 하면 그사이에 다른 프록시나 데코레이터가 계속 추가 될 수 있기 때문이다.

## 다이내믹 프록시 

프록시를 만드는 일이 상당히 번거 롭게 느껴진다 . 왜냐하면 매번 새로운 클래스를 정의 해야하고 , 인터페이스의 구현해야할 메소드는 많으면 모든 메소드를 일일히 구현해서 위임하는 코드를 넣어야하기 때문이다.

물론 자바에는 java.lang.reflect 패키지 안에 프록시를 손쉽에 만들수 있도록 지원해 주는 클래스들이 있다.

기본적인 아이디어는 목 프레임 워크와 비슷하다 일일이 프록시 클래스를 정의하지 않고도 몇 가지 api를 이용해 프록시 처럼 동작하는 오브젝트를 다이내믹 하게 생성하는 것이다.

> 트랜잭션 부가기능을 위해 만든 UserTx는 기능 부가를 위한 프록시다. 아래 코드에 UserTx코드에서 이 두가지 기능을 구분해보자.

```java
pulic class UserServiceTx implements UserService{
	UserService userService; // 타깃 오브젝트
	...
	public void add(User user){ //메소드 구현과 위임
		this.userService.add(user);
	}
	public void updradeLevels(){ //메소드 구현
		TransactionStatus suatus = this.transactionManager //부가기능 수행
			.getTransaction(new DefaultTransactionDefinition());
		try{
			userService.upgradeLevels(); //위임
			
			this.transactionManager.commit(status); //부가기능수행
		}catch(RuntimeException e){
			this.transactionManager.rollback(status);
			throw e;
		}
	}
}
```

## 리플렉션

다이내믹 프록시는 리플렉션 기능을 이용해서 프록시를 만들어 준다 . 리플렉션은 자바의 코드 자체를 추상화해서 접근하도록 만든것이다.

자바의 모든 클래슨느 그 클래스 자체의 구성정보를 담은 class 타입의 오브젝트를 하나씩 갖고 있다 . '클래스이름 .class' 라고 하거나 오브젝트의 getclass() 메소드를 호출 하면 클래스 정보를 담은 class 타입의 오브젝트를 가져올수있다.

쉽게 정리하자면 컴파일 시점이 아니라 런타임시에 동적으로 객체를 생성하는 기술이다.

### 프록시 클래스

> 다이내믹 프록시를 이용한 프록시를 만들어보자 .  프록시를 적용할 타깃 클래스와 인터페이스를 정의한다

```
interface Hello{
	String sayHello(String name);
	String sayHi(String name);
	String sayThankYou(String name);
}
```

> 이를 구현한 타깃 클래스

```
public class HelloTarget implements Hello{
	public String sayHello(String name){
		return "Hello" + name;
	}
	public String sayHi(String name){
		return "Hi" + name;
	}
	public String sayThankYou(String name){
		return "Thank You" + name;
	}
}
```

> 클라이언트 역할의 테스트

```java
@Test
public vlid simpleProxy(){
	Hello hello = new HelloTarget();
	assertThat(hello.sayHello("Toby"),is(Hello Toby));
	assertThat(hello.sayHi("Toby"),is(Hi Toby));
	assertThat(hello.sayThankYou("Toby"),is(Thank You Toby));
}
```

이제 Hello 인터페이스를 구현한 프록시를 만들어 보자 . 프록시 에는 데코레이터 패턴을 적용해서 타깃인 HelloTarget에 부가기능을 추가하겠다. 프록시의 이름은 HelloUppercase다. 

> 위임과 기능 부가라는 두가지 프록시의 기능을 모두 처리하는 전형적인 프록시 클래스인HelloUppercase

```java
public class HelloUppercase implements Hello{
	Hello hello; //위임할 타깃 프로젝트, 타깃 클래스의 오브젝트인 것은 알지만 다른 프록시를 추가할수도 있으므로 인터페이스로 접근
	
	public HelloUppercase(Hello hello){
		this.hello = hello;
	}
	public String sayHello(String name){
		return hello.satHello(name).toUpperCase(); //위임과 부가기능 적응
	}
	public String sayHello(String name){
		return hello.sayHi(name).toUpperCase();
	}
	public String sayHello(String name){
		return hello.sayThankYou(name).toUpperCase();
	}
}
```

> HelloUppercase 프록시 테스트

```java
Hello proxiedHello = new HelloUppercase(new HelloTarget()); 
// 프록시를 통해 타깃 오브젝트에 접근하도록 구성
assertThat(proxiedHello.sayHello("Toby"),is(HELLO TOBY));
assertThat(proxiedHello.sayHi("Toby"),is(HI TOBY));
assertThat(proxiedHello.sayThankYou("Toby"),is(THANK YOU TOBY));
```

**이 프록시는 프록시 적용의 일반적인 문제점 두가지를 모두 갖고 있다 . 인터페이스의 모든 메소드를 구현해 위임하도록 코드를 만들어야하며 , 부가적인 리턴 값을 대문자로 바꾸는 기능이 모든 메소드에 중복돼서 나타난다.**

## 다이나믹 프록시 적용

클래스로 만든 프록시인 HelloUppercase를 다이내믹 프록시를 이용해 만들어보자.

다이내믹 프록시는 프록시 팩토리에 의해 **런타임 시 다이내믹하게 만들어지는 오브젝트다 .**다이내믹 프록시 오브젝트는 타깃의 인터페이스와 같은 타입으로 만들어진다 . 클라이언트는 다이내믹 프록시 오브젝트를 타깃 인터페이스를 통해 사용 할수 있다. 이 덕분에 프록시를 만들 때 **인터페이스를 모두 구현해 가면서 클래스를 정희하는 수고를 덜 수 있다.**

다이내믹 프록시가 인터페이스 구현 클래스의 오브젝트는 만들어 주지만,**프록시로서 필요한 부가기능 제공 코드는 직접 작성해야한다 . **부가기능은 프록시 오브젝트와 독립적으로 InvocationHandler를 구현한 오브젝트에 담는 InvocationHandler인터페이스는 다음과같은 메소드 한개만 가진 간단한 인터페이스다.

```java
public Object invoke(Object proxy,Method mothod,Object[] args)
```

invoke() 메소드는리플렉션의 Method 인터페이스를 피라미터로 받는다. 메소드를 호출 할 때 전달 되는 파라미터로 args로 받는다 . **다이내믹 프록시 오브젝트는 클라이언트의 모든 요청을 리플렉션 정보로 변환해서 InvocationHandler구현 오브젝트의 invoke() 메소드로 넘기는것이다. **

Hello 인터페이스를 제공하면서 프록시 팩토리에게 다이내믹 프록시를 만들어 달라고 요청하면 Hello 인터페이스의 모든 메소드를 구현한 오브젝트를 생성해준다 .InvocationHandler 인터페이스를 구현한 오브젝트를 제공해주면 다이내믹 프록시가 받는 모든 요청을 InvocationHandler의 invoke() 메소드로 보내준다. 

> 다이내믹 프록시를 만들어보자 . 먼저 다이내믹 프록시로 부터 메소드 호출 정보를 받아서 처리하는 InvocationHandler 를 만들어보자.

```java
public class UppercaseHandler implements InvocationHandler{
	Hello target;
	
	public uppercaseHandler(Hello target){
		this.target =target;
	}
	
	public Object invoke(Object proxy,Method mothod,Object[] args)throws Throwable{
		String ret = (String)method.invoke(target,args);
		//타깃으로 위임 . 인터페이스의 메소드 호출에 모두 적용된다.
		return ret.toUpperCase(); //부가기능 제공
	}
}
```

> 이제 이 InvocationHandler를 사용하고 Hello 인터페이스를 구현하는 프록시를 만들어보자 .

```java
Hello proxeHello = (Hello)Proxy.newProxtInstance(
	getClass().getClassLoader(), //동적으로 생성되는 다이내믹 프록시 클래스의 로딩에 사용할 클래스로더
	new Class[]{Hello.class}, //구현할 인터페이스
	new UppercaseHandler(new HelloTarget()) //부가기능과 위임 코드를 담은 InvocationHandler
);
```

### 다이내믹 프록시의 확장

UppercaseHandler는 모든 메소드의 리턴 타입이 스트링이라고 가정한다. 그런데 스트링 외의 리턴 타입을 갖는 메소드가 추가되면 어떨까? 지금은 강제로 스트링으로 캐스팅을 해버리니 런타임 시에 캐스팅 오류가 발생할 것이다. 그래서 Method를 이용한 타깃 오브젝트의 메소드 호출 후 리턴 타입을 확인해서 스트링인 경우만 대문자로 바꿔주  고 나머지는 그대로 넘겨주는 방식으로 수정하는 것이 좋겠다.

```java
public class UppercaseHandler implements InvocationHandler{
	Object target;
	private UppercaseHandelr(Object target
                             
                             
                             
                             
                             
                             
		//호출한 메소드의 리턴타입이 String 인 경우에만 대문자 변경 기능을 적용하도록 수정
	}
}
```

InvocationHandle는 단일 메소드에서 모든 요청을 처리하기 때문에 어떤 메소드에 어떤 기능을 적용 할지를 선택하는 과정이 필요할수도있다 . 호출하는 메소드의 이름 파라미터의 개수와 타입, 리턴 타입등의 정보를 가지고 부가적인 기능을 적용할 메소드를 선택할수 있다.리턴 타입뿐 아니라 메소드의 이름도 조건으로 걸수있다.

> 메소드를 선별해서 부가기능을 적용하는 invoke()

```java
public Object invoke(Object proxy,Method method,Object[] args) throws Throwable{
		Object ret = method.invoke(trget,args);
		if(ret instanceof Stirng && method.getName().startsWith("say")){
		//리턴타입과 메소드 이름이 일치하는 경우에만 부가기능을 적용
			return((String)ret).toUpperCase();
		}else{
			return ret;
		}
	}
```

## 다이내믹 프록시를 이용한 트랜잭션 부가기능

UserServiceTx를 다이내믹 록시 방식으로 변경해보자. UserServiceTx 는 서비스 인터페이스의 메소드를 모두 구현해야 하고 트랜잭션이 필요한 메소드마다 트랜잭션 처리코드가 중복돼서 나타나는 비효율적인 방법으로 만들어져있다 . 따라서 트랜잭션 부가기능을 제공하는 다이내믹 프록시를 만들어 적용하는 방법이 효율적이다 . 

### 트랜잭션 InvocationHandler

> 트랜잭션 부가기능을 가진 핸들러의 코드는 아래와 같이 정의할수 있다.

```java
public class TrancationHandler implements InvocationHandler{
	private Object target;
	private Object PlatformTransactionManager transactionManager;
	private String paattern;
	
	public void setTargetTarget(Object target){
		this.target = target;
	}
	
	public void setTransactionManaget(PlatformTransactionManager transactionManager){
		this.transactionManager = transactionManager;
	}
	
	public void setPattern(String pattern){
		this.pattern = pattern
	}
	
	public Object invoke(Object proxy,Method method,Object[] args)throws Throwable{
        //트랜잭션 적용 대상 메소드를 선별해서 트랜잭션 경계 설정 기능을 부여해준다.
		if(method.getName(),statusWith(pattern)){
			return invokeInTransaction(method,args);
		}else{
			return method.invoke(target,args);
		}
	}
	
	private Object invokeInTransaction(Method method,Object[] args)throws Throwable{
		TransactionStatus status =
			this.transactionManager.getTransaction(new DefaultTransactionDefinition());
		try{ //트랜잭션을 시작하고 타깃 오브젝트의 메소드를 호출.
			Object ret = method.invoke(target,args);
			this.transactionManager.commit(status);
			return ret;
		}catch(InvocationTargetExcetion e){
			this.transactionManager.rollback(status);
			throw e.getTargetException();
		}
	}
}
```

요청을 위임할 타깃을 DI로 제공 받도록 한다 . 타깃을 저장할 변수는 Object로 선언했다 . 따라서 UserServiceImpl 외에 트랜잭션 적용이 필요한 어떤 타깃 오브젝트에도 적용 할수 있다 .

PlatformTransactionManager를 DI 받도록 하고 , 트랜잭션을 적용할 메소드 이름의 패턴을 DI받는다.

DI 받은 이름 패턴으로 시작되는 이름을 가진 메소드인지 확인하고 , 패턴과 일치하는 이름을 가진 메소드라면 트랜잭션을 적용 하는 메소드를 호출 하고 , 아니라면 부가기능 없이 타깃 오브젝트의 메소드를 호출해서 결과를 리턴하게 한다.

### TrancactionHandler와 다이내믹 프록시를 이용하는 테스트

```java
@Test
public void upgradeAllOrNothing() throws Exception{
	...
	TransationHandler txHandler = new TransactionHandler();
	txHandler.setTarget(testUserService);
	txHandler.setTransactionManager(transactionManager);
	txHandler.setPattern("upgradeLevels");
    //트랜잭션 핸들러가 필요한 정보와 오브젝트 di를 해준다.
	UserService txUserService = (UserService)Proxy.newProxyInstance(
		get.class().getClassLoader(),new Class[]{UserService.class},txHandler
	); //UserService 인터페이스 타입의 다이내믹 프록시 생성
}
```

## 다이내믹 프록시를 위한 팩토리 빈

이제 TransactionHandler와 다이내믹 프록시를 스프링의 DI를 통해 사용할 수 있도록 만들어야할 차례다.

그런데 문제는 DI의 대상이 되는 다이내믹 프록시 오브젝트는 일반 적인 스프링의 빈으로는 등록할 방법이 없다는 점이다.

스프링은 내부적으로 리플렉션 API를 이용해서 빈 정의에 나오는 클래스 이름을 가지고 빈 오브젝트를 생성한다 . 문제는 다이내믹 프록시 오브젝트는 이런식으로 프록시 오브젝트가 생성 되지 않는다는 점이다. 사실 다이내믹 프록시 오브젝트의 클래스가 어떤 것인지도 알 수도없다 . 클래스 자체도 내부적으로 다이내믹하게 새로 정의해서 사용 하기 때문이다 .

## 팩토리 빈

사실 스프링은 클래스 정보를 가지고 디폴트 생성자를 통해 오브젝트르르 만드는 방법 외에도 빈을 만들 수 있는 여러가지 방법을 제공한다 . 대표적으로 팩토리 빈을 이용한 빈생성 방법을 들 수있다 .

> 팩토리 빈을 만드는 방법에는 여러가지가 있는데 , 가장 간단한 방법은 스프링의 FactoryBean이라는 인터페이스를 구현하는것이다.

```java
public interface FactoryBean<T>{
	T getObject() throws Exception; //빈오브젝트를 생성해서 돌려준다 .
	Class<? extends ?> getObjectType(); // 생성되는 오브젝트 타입을 알려준다.
	boolean isSingleton(); // getObject()가 돌려주는 오브젝트가 항상 같은 싱글톤인지 확인 
}
```

> FactotyBean 인터페이스를 구현한 클래스를 스프링의 빈으로 등록하면 팩토리 빈으로 동작한다. 팩토리 빈의 동작 원리를 확인 할 수있도록 만들어진 학습테스트를 하나 살펴보자.

```java
public class Massage{
	String text;
	
	private Message(String text){ //생성자가 private으로 선언되어 있어서 외부에서 생성자사용 x
		this.text = text;
	}
	
	public String getText(){
		return text;
	}
	
	public static Message newMessage(String text){ //생성자 대신 사용할 수 있는 팩토리 메소드제공
		return new Message(text);
	}
}
```

Massage 클래스의 오브젝트를 만들려면 newMessage() 라는 스태틱 메소드를 사용해야 한다 . 따라서 이 클래스를 직접 스프링 빈으로 등록해서 사용 할수 없다.

사실 스프링은 private 생성자를 가진 클래스도 빈으로 등록해주면 리플렉션을 이용해 오브젝트를 만들수 있지만 . 생성자를 private로  만들었다는건 스태틱 메소드를 통해 오브젝트가 만들어져야하는 중요한 이유가 있기 때문이므로 이를 무시하고 오브젝트를 강제로 생성하면 위험하다. 

> Massage클래스의 오브젝트를 생성해주는 팩토리 빈 클래스를 만들어보자

```java
public class MessateFactoryBean implements FactoryBean<Message>{
	String text;
	
	/*
	오브젝트를 생성 할때 필요한 정보를 팩토리 빈의 프로퍼티로 설정해서 대신 DI 받을수 있게한다.
	*/
	public void setText(String text){
		this.text = text;
	}

	/*
    실제 빈으로 사용될 오브젝트를 직접 생성한다 . 코드를 이용하기 때문에 복잡한 방식의 오브젝트 생성과
    초기화 작업도 가능하다.
	*/
	public Message getObject() throws Exception{
		return Massage.newMessage(this.text);
	}
	
	public Class<? extends Message>getObjectType(){
		return Message.class;
	}
	
	public boolean isSingleton(){
		return false;
	}
}
```

여타 빈 설정과 다른점은 **message빈 오브젝트 타입이 class 애트리뷰트에 정의된 MessageFactoryBean이 아니라 Message 타입 이라는 것이다.**Message 빈의 타입은 MessageFactoryBean의 getObjectType() 메소드가 돌려주는 타입으로 결정된다. 또 getObject() 메소드가 생성해주는 오브젝트가 message빈의 오브젝트가된다.

### 트랜잭션 프록시 팩토리 빈

> TransavtionHandler를 이용하는 다이내믹 프록시를 생성하는 팩토리빈 클래스 

```java
public class TxProxyFactoryBean implements FactoryBean<Object>{
	Object target;
	PlatfromTransactionManager transactionManager;
	String pattern;
	Class<?> serviceInterface;
	
	public void setTarget(Object target){
		this.target = target;
	}
	
	public void setTransactionManager(PlatformTransactionManager transactionManager){
		this.transactionManager = transactionManager;
	}
	
	public void setPattern(String pattern){
		this.pattern = pattern;
	}
	
	public void setServiceInterface(Class<?> serviceInterface){
		this.servieInterface = serviceInterface;
	}
	
	public Object getObject() throws Exception{
		TransationHandler txHandler = new TransactionHandler();
		txHandler.setTarget(target);
		txHandler.setTrancactionManager(transactionManager);
		txHandler.setPattern(pattern);
		return Proxy.newProxyInstance(
			getClass().getClassLoader(),new Class[] {serviceInterface},txHandler
		);
	}
	
	public Class<?> getObjectType(){
		return serviceInterface;
	}
	
	public boolean isSigleton(){
		return false;
	}
}
```

> UserService 에 대한 트랜잭션 프록시 팩토리 빈

```java
<bean id="userService" class= pringbook.user.service TxProxyFactoryBean">
	<property name="target" ref="userServicelmpl" />
	<property name="transactionManager" ref="transactionManager“ /> 
	<property name="pattern" value="upgradeLevels" /> 
	<property name="servicelnterface" value= pringbook user.service.UserService" /> 
</bean>
```

### 프록시 팩토리 빈의 한계 

- 하나의 클래스 안에 존재하는 여러 개의 메소드에 부가기능을 한 번에 제공하는건 가능했으나 , 한번에 여러개의 클래스에 공통 적인 부가기능을 제공하는 방법은 불가능
- 하나의 타깃에 여러개의 부가기능을 적용 하려고 할때도 프록시 팩토리 빈 설정이 부가 기능이 갯수만큼 따라 붙어야함.
- TransactionHandler 오브젝트가 프록시 팩토리 빈 개수 만큼 만들어진다는점 . TransactionHandler 는 타깃오브젝트를 프로퍼티로 가지고 있다 . 따라서 트랜잭션 부가기능을 제공하는 동일한 코드임에도 불구하고 타깃 오브젝트가 달라지면 새로운 TransactionHandler 오브젝트를 만들어야한다.

## 스프링의 프록시 팩토리빈

자바 에서는 JDK에서 제공하는 다이내믹 프록시 외에도 편리하게 프록시를 만들 수 있도록 지원해주는 다양한 기술이 존재한다 . **따라서 스프링은 일관된 방법으로 프록시를 만들 수 있게 도와주는 추상 레이어를 제공한다.** 생성된 프록시는 스프링의 빈으로 등록돼야한다 . 스프링은 프록시 오브젝트를 생성해주는 기술을 추상화한 팩토리빈을 제공해준다.

### ProxyFactoryBean

스프링의 ProxyFactoryBean은 프록시를 생성해서 빈 오브젝트로 등록 하게 해주는 팩토리 빈이다 . 기존에 만들었던 TxProxyFactoryBean과 달리, ProxyFactoryBean은 순수하게 프록시를 생성하는 작업만들 담당하고 프록시를 통해 제공 해줄 부가기능은 별오의 빈에 둘수 있다.

Proxy FactoryBean 이 생성하는 프록시에서 사용할 부가기능은 Methodlnterceptor  인터페이스를 구현해서 만든다. Methodlnterceptor는 InvocationHandler와 비슷하지만 한 가지 다른점이 있다.

  InvocationHandler 의 invoke() 메소드와 다르게 Methodlnterceptor 의 invoke() 메소드는  ProxyFactoryBean으로부터 타깃 오브젝트에 대한 정보까지 함께 제공받는다.

```java
public class DynamicProxyTest{
	@Test
	public void simpleProxy(){
		Hello proxiedHello = (Hello)Proxy,newProxtInce(
			getClass().getClassLoader(),
			new Class[] {Hello.class},
			new UppercaseHandler(new HelloTarget())
		);
		...
	}
	@Test
	public void proxtFactoryBean(){
		ProxyFactoryBean pfBean = new ProxyFactoryBean();
		pfBean.setTarget(new HelloTarget());
		pfBean.addAdvice(new UpeercaseAdvice());
		
		Hello proxiedHello = (Hello) pfBean.getObject();
		assertThat(proxiedHello.sayHello("Toby"),is("HEllO TOBY')); 
		assertThat(proxiedHello.sayHi("Toby"),is( "HI TOBY")); 
		assertThat(proxiedHello.sayThankYou("Toby"),is("THANK YOU TOBY"))
	}
	
	static class UppercaseAdvice implements MethodInterceptor{
		public Object invoke(MethodInvocation invocation) throws Throwable{
			String ret = (String)invocation.proceed();
			return ret.toUpperCase();
		}
	}
	
	static interface Hello{
		String sayHello(String name); 
		String sayHi(String name); 
        String sayThankYou(String name)
	}
                                                    
	static class HelloTarget implements Hello { 
		public String sayHello(String name) { return "Hello" + name; } 
		public String sayHi(String name) { return "Hi" + name; } 
		public String sayThankYou(String name) { return "Thank You" + name; }
	}
}
```

### 어드바이스 : 타깃이 필요 없는 순수한 부가기능

Proxy FactoryBean을 적용한 코드를 기존의 JDK 다이내믹 프록시를 사용했던 코드와  비교해보면 몇 가지 눈에 띄는 차이점이 있다.

- UppercaseAdvice 는 타깃 오브젝트가 등장하지 않고 ,  Methodlnterceptor 는 메소드 정보와 함께 타깃 오브젝트가 담긴 Methodlnvocation 오브젝트가 전달된다. Methodlnvocation은 타깃 오브젝트의 메소드를 실행할 수 있는 기능이 있기 때문에  Methodlnterceptor는 부가기능을 제공하는 데만 집중할 수 있다.

  Methodlnvocation은 일종의 콜백 오브젝트로， proceed() 메소드를 실행하면 타깃  오브젝트의 메소드를 내부적으로 실행해주는 기능이 있다. 그렇다면 Methodlnvocation  구현 클래스는 일종의 공유 가능한 템플릿처럼 동작히는 것이다.

- ProxyFactoryBean 에 이 Methodlnterceptor를 설정해줄 때는 일반적인 DI 경우처럼 수정자 메소드를 사용하는 대신 addAdvice() 라는 메소드를 사용한다.

  dd 라는 이름에서 알 수 있듯이 ProxyFactoryBea 에는 여러 개의  Methodlnterceptor를 추가할 수 있다. ProxyFactoryBean 하나만으로 여러 개의 부가기능을 제공해주는 프록시를 만들 수 있다는 뜻이다.

- ProxyFactoryBean을 적용한 코  드에는 프록시가 구현해야 하는 Hello 라는 인터페이스를 제공해주는 부분이 없다.

  인터페이스를 굳이 알려주지 않아도 ProxyFactoryBean 에 있는 인터페이스 자동검출 기능을 사용해 타깃 오브젝트가 구현하고 있는 인터페이스 정보를 알아낸다. 그리고 알아  낸 인터페이스를 모두 구현히는 프록시를 만들어준다.

  ProxyFactoryBean은 기본적으로 JDK가 제공히는 다이내믹 프록시를 만들어준다.

### 포인트컷 : 부가기능 적용 대상 메소드 선정 방법

메소드의 이름과 패턴을 비교해서 부가기능인 트랜잭션 적용 대상을 판별했다.  그렇다면 스프링의 ProxyFactoryBean과 Methodlnterceptor를 사용하는 방식에서도  메소드 선정 기능을 넣을 수 있을까?

Methodlnterceptor 오브젝트는 여러 프록시가 공유해서 사용할 수 있다. 그러기 위해  서 Methodlnterceptor 오브젝트는 타깃 정보를 갖고 있지 않도록 만들었다. 그렇기 때문에 패턴은 프록시마다 다를 수 있기 때문에 여러 프록시가 공유하는 Methodlnterceptor에  특정 프록시에만 적용되는 패턴을 넣으면 문제가 된다. 

Methodlnterceptor에는 재사용 가능한 순수한 부가기능 제공 코드만 남겨주고  대신 프록시에 부가기능 적용 메소드를  선택히는 기능을 넣자.

**스프링은 부가기능을 제공하는 오브젝트를 어드바이스라고 부르고 , 메소드 선정 알고리즘을 담은 오브젝트를 포인트컷이라고 부른다 .** 어드바이스와 포인트 것은 모두 프록시에게 DI로 주입돼서 사용된다.

프록시는 클라이언트로 부터 요청을 받으면 

1. 먼저 포인트 컷에게 부가기능을 부여할 메소드인지 확인해달라고 요청한다.
2. 프록시는 포인트 컷으로부터 부가기능을 적용할 대상 메소드인지 확인 받으면 Methodlnterceptor 타입의 어드바이스를 호출한다.

어드바이스는 JDK의 다이내믹  프록시의 InvocationHandler와 달리 직접 타깃을 호출하지 않는다. 자신이 공유돼야  하므로 타깃 정보라는 상태를 가질 수 없다. 따라서 타깃에 직접 의존하지 않도록 일종의 템플릿 구조로 설계되어 있다.

어드바이스가 부가기능을 부여히는 중에 타깃 메소드  의 호출이 필요하면 프록시로부터 전달받은 Methodlnvocation 타입 콜백 오브젝트의  proceed() 메소드를 호출해주기만 하면 된다. 

> 포인트컷까지 적용한 ProxyFactoryBean

```JAVA
@Test
public void pointcutAdvisor(){
	ProxyFactoryBean pfBean = new ProxyFactoryBean();
	pfBean.setTarget(new HelloTarget());
	
	NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
	//메소드 이름을 비교해서 대상을 선정하는 알고리즘을 제공하는 포인트컷 생성
	pointcut.setMappedName("sayH*"); //sayH로 시작하는 모든 메소드를 선택하게함
	
	pfBean.addAdvisor(new DefaultPointcutAdvisor(pointsut,new UppercaseAdvice()));
	//포인트컷과 어드바이스를 Advisor로 묶어서 한번에추가 
	Hello proxideHello = (Hello)pfBean.getObject();
	
	assertThat(proxiedHello.sayHello("Toby"),is("HELLO TOBY"));
	assertThat(proxiedHello.sayHI("Toby"),is("HI TOBY"));
	assertThat(proxiedHello.sayThankYou("Toby"),is("Thank You TOBY"));
	//조건에 부합하지 않으므로 대문자변환이 되지 않는다.
}
```

 ProxyFactoryBean에는 여러 개의 어드바이스와 포인트컷이 추가될 수 있기 때문에 포인트컷을 함께 등록할 때는 어드바이스와 포인트  컷을 Advisor 타입으로 묶어서 addAdvisor() 메소드를 호출해야 한다.

**이렇게 어드바이스와 포인트컷을 묶은 오브젝트를 인터페이스 이름을 따서 어드바이저라고 부른다.**

###  ProxyFactoryBean 적용

부가기능을 담당히는 어드바이스는 테스트에서 만들어본 것처럼 Methodlnterceptor 라는 Advice 서브인터페이스를 구현해서 만든다.

> JDK 다이내믹 프록시 방식으로 만든 TransactionHandler의 코드에서 타깃과 메소드 선정 부분을 제거 

```java
public class TransactionAdvice implements MethodInterceptor{
    // 스프링의 어드바이스 인터페이스 구현
    
	PlatformTransactionManager transactionManager;
	
	public void serTransactionManager(PlatformTransactionManager transactionManager){
		this.transactionManager = tranaactionManager;
	}
	//타깃을 호출하는 기능을 가진 콜백 오브젝트를 프록시로부터 받는다 .
    //덕분에 어드바이스는 특정 타깃에 의존하지 않고 재사용 가능하다.
	public Object invoke(MethodInvocation invocation)throws Throwable{
		TransactionStatus status = this.transactionManager.getTransacation(new FefaultTransactionDefiniton());
		try{
			Object ret = invocation.proceed();
			this.transactionManager.commit(status);
			return ret;
		}chath(RuntimeException e){
			this.transactionManager.rollback(status);
			throw e;
		}
	}
}
```

> ProxyFacIαyBean 을 이용한 트랜잭션 테스트 

```java
@Test
@DirtiesContext
public void uparadeAllOrNothing(){
	TestUesrService tsetUserSerice = new TserUserService(users.get(3).getId());
	testUserService.setUserDao(userDao);
	
	ProxyFactoryBean txProxyFactoryBean =
		context.getBean("&userService",ProxtFactory.class);
	txProxyFactoryBean.setTarget(testUserService);
	UserService txUserService = (UserService) txProxtFactoryBean.getObject();
	...
}
```

## 스프링 AOP

지금 까지 해왔던 작업의 목표는 비즈니스 로직에서 반복적으로 등장해야만 했던 트랜잭션 코드를 깔끔하고 효과적으로 분리해내는 것이다.  투명한 부가기능을 적용하는 과정에서 발견됐던 거의 대부분의 문제는 제거했다 . 남은것은 부가기능의 적용이 필요한 타깃 오브젝트마다 거의 비슷한 내용의 ProxyFactoryBean 빈 설정정보를 추가해주는 부분이다. 새로운 타깃이 등장했다고 해서 코드를 손댈 필요는 없어졌지만， 설정은 매번 복사해서 붙이고 target 프로퍼티의  내용을 수정해줘야 한다.

### 빈 후처리기를 이용한 자동 프록시 생성기

 스프링은 컨테이너로서 제공하는 기능 중에서 변하지 않는 핵심적인 부분  외에는 대부분 확장할 수 있도록 확장 포인트를 제공해준다. 

그중에서 관심을 가질 만한 확장 포인트는 바로 BeanPostProcessor 인터페이스를  구현해서 만드는 빈 후처리기다. 빈 후처리기는 이름 그대로 스프링 빈 오브젝트로 만  들어지고 난 후에， 빈 오브젝트를 다시 가공할 수 있게 해준다.

DefaultAdvisorAutoProxyCreator는 어드바이저를 이용한 자동 프록시 생성기이다.

 빈 후처리기를 스프링에 적용히는 방법은 간단하다. 빈 후처리기 자체를 빈으로 등록히는 것  이다. 스프링은 빈 후처리기가 빈으로 등록되어 있으면 빈 오브젝트가 생성될 때마다 빈  후처리기에 보내서 후처리 작업을 요청한다.

1. 빈 후처 리기가 등록되어 있으면 스프링은 빈 오브젝트를 만들 때마다 후처리기에게 빈을 보낸다.
2.  빈  으로 등록된 모든 어드바이저 내의 포인트컷을 이용해 전달받은 빈이 프록시 적용 대상인지 확인한다.
3.  프록시 적용 대상이면 그때는 내장된 프록시 생성기에게 현재 빈에 대한 프록시를 만들게 하고 만들어진 프록시에 어드바이저를 연결해준다.
4. 빈 후처리기는  프록시가 생성되면 원래 컨테이너가 전달해준 빈 오브젝트 대신 프록시 오브젝트를 컨테이너에게 돌려준다.
5.  컨테이너는 최종적으로 빈 후처기가 돌려준 오브젝트를 빈으로  등록하고시용한다.

포인트컷은 은 클래스 필터와 메소드 매처 두 가지를  돌려주는 메소드를 갖고 있다. 기존에 사용한 NameMatchMethodPointcut은 메소드  선별 기능만 가진 특별한 포인트컷이다. 메소드만 선별한다는 건 클래스 필터는 모든  클래스를 다 받아주도록 만들어져 있다는 뜻이다. 따라서 클래스의 종류는 상관없이 메소드만 판별한다. 어차피 ProxyFactoryBean 에서 포인트컷을 사용할 때는 이미 타깃이 정해져 있기 때문에 포인트컷은 메소드 선별만 해주면 그만이었다. 

 모든 빈에 대해 프록시 자동 적용 대상을 선별해야 하는 빈 후처리기인  DefaultAdvisorAutoProxyCreator는 클래스와 메소드 선정 알고리즘을 모두 갖고 있는  포인트컷이 필요하다. 정확히는 그런 포인트컷과 어드바이스가 합되어 있는 어드바이저가 등록되어 있어야한다. 

### 어드바이스와 어드바이저

이제는 ProxyFactoryBean으로 등록한 빈에서처럼  transactionAdvisor를 명시적으로 Di 하는 빈은 존재하지 않는다. 

대신 어드바이저를  이용하는 자동 프록시 생성기인 DefaultAdvisorAutoProxyCreator에 의해 자동수집되고， 프록시 대상 선정 과정에 참여하며 자동생성된 프록시에 다이내믹하게 DI 돼서 동작하는 어드바이저가 된다.

### 자동 프록시 생성기를 사용하는 테스트

> 테스트용 UserService 구현클래스

```java
static class TestUesrServiceImpl extends UserSericeImpl{
	private Stirng id = "madnite1";
	
	protectd void upgradeLevel(User user){
		if(user.getId().eqals(this.id)) throw new TestUserServiceException();
		super.upgradeLevel(user);
	}
}
```

> 포인트컷 빈 

```java
<bean id="transactionPointcut" 
		class="springbook .service.NameMatchClassMethodPointcut">
	<property name="mappedClassName" value="*Servicelmpl" />
	<property name="mappedName" value=“upgrade*" />
</bean>
```

> 테스트용 UserService 등록

```xml
<bean id = "testUserService" 
	class= "springbook.user.service.UserServiceTest$TestUserServiceImpl" 
	parent="userService" /> <!-- 빈의 설정을 상속받음 -->
```

> testUserService 빈을 사용하도록 수정된 테스트 

```java
public class UserSericeTset{
	@Autowrid UserService userService;
	@Autowrid UserService testUserService;
	//userService 빈과 타입이 중복되므로 타입뿐 아니라 변수 이름을 빈과 일치시켜준다
	
	@Test
	public void upgradeAllOrNothing(){
		userDao.dlelteAll();
		for(User user : users) userDao.add(user);
		try{
			this.tesetUserService.upgradeLevels();
			fail("TestUserServiceException expected");
		}catch(TestUserServiceException e){
		
		}
		checkLevelUpgeraded(users.get(1),false);
	}
}
```

##  AOP란 무엇인가?

전통적인 객체지향 기술의 설계 방법으로는 독립적인 모률화가 불가능한 트랜잭션 경계설정과 같은 부가기능을 어떻게 모율화할 것인가를 연구해온 사람들은， 이 부가기능 모율화 작업은 기존의 객체지향 설계 패러다임과는 구분되는 새로운 특성이 있다고 생각했다. 그래서 이런 부가기능 모율을 객체지향 기술에서 주로 사용하는 오브젝  트와는 다르게 특별한 이름으로 부르기 시작했다. 그것이 바로 애스펙트다.

애스펙트란 그 자체로 애플리케이션의 핵심기능을 담고 있지는 않지만， 애플리케이션을  구성하는 중요한 한가지 요소이고 핵심기능에 부가되어 의미를 갖는 특별한 모듈을  가리킨다.

애스펙트는 부가될 기능을 정의한 코드인 어드바이스와， 어드바이스를 어디에 적용할지를 결정하는 포인트컷을 함께 갖고 있다. 지금 사용하고 있는 어드바이저는 아주  단순한 형태의 애스펙트라고 볼 수 있다. 

이렇게 애플리케이션의 핵심적인 기능에서 부가적인 기능을 분리해서 애스펙트라  는 독특한 모율로 만들어서 설계하고 개발하는 방법을 애스펙트 지항 프로그래밍(Aspect  Oriented Programming )또는 약자로 AOP라고 부른다. AOP는 OOP를 돕는 보조적인  기술이지 OOP를완전히 대체하는 새로운개념은아니다.

## 프록시를 이용한 AOP

스프링은 IoC/DI 컨테이너와 다이내믹 프록시， 데코레이터 패턴， 프록시 패턴， 자동 프록시 생성 기법， 빈 오브젝트의 후처리 조작 기법 등의 다양한 기술을 조합해 AOP를  지원하고 있다.

스프링 AOP의 부가기능을 담은 어드바이스가 적용되는 대상은 오브젝트의 메소드다. 프록시 방식을 사용했기 때문에 메소드 호출 과정에 참여해서 부가기능을 제공해주게 되어 있다.

## AOP의 용어

타깃 - 부가기능을 부여할대상(핵심기능을 담은 클래스 일 수도 있지만 경우에 따라서 다른 부가기능을 제공하는 프록시 오브젝트 일수도 있다.)

어드바이스 - 어드바이스는 타깃에게 제공할 부가기능을 담은 모듈이다.

조인포인트 - 어드바이스가 적용될 수 있는 위치를 말한다. 스프링의 프록시 AOP에서 조인포인트는 메소드의 실행단계 뿐이다.  타깃 오브젝트가 구현한 인터페이스의 모든 메소드는 조인포인트가 된다.

포인트컷 - 어드바이스를 적용할 조인 포인트를 선별하는 작업 또는 그 기능을 정의한 모듈.

프록시 - 프록시는 클라이언트와 타깃 에에 투명하게 존재하면서 부가기능을 제공히는 오브젝트. DI를 통해 타깃 대신 클라이언트에게 주입되며 , 클라이언트의 메소드 호출을 대신 받아서 타깃에게 위임 해주며, 그과정에서 부가기능을 부여함

어드바이저 - 포인트컷과 어드바이스를 하니씩 가지고 있는 오브젝트 (AOP의 가장 기본이 되는 모듈)

애스팩트 -AOP의 기본 모듈이다. 스프링의 어드바이저는 아주 단순한 애스팩트라고 볼수있다.

### AOP의 네임스페이스 

자동 프록시 생성기  -  애플리케이션 컨텍스트가 빈 오브젝트를 생성히는 과정에 빈 후처리기  로 참여한다. 빈으로 등록된 어드바이저를 이용해서 프록시를 자동으로 생성하는 기능을담당한다. 

어드바이스 - 부가기능을 구현한 클래스를 빈으로 등록한다. TransactionAdvice는 AOP 관련 빈  중에서 유일하게 직접 구현한 클래스를 사용한다.

포인트컷 - 스프링의 AspectJExpressionPointcut을 빈으로 등록하고 expression 프로퍼티에  포인트컷 표현식을 넣어주면 된다.

어드바이저 - 스프링의 DefaultPointcutAdvisor 클래스를 빈으로 등록해서 사용한다. 어드바이스와 포인트컷을 프로퍼티로 참조하는 것 외에는 기능은 없다.

# 6장 트랜잭션

트랜잭션이라고 모두 같은 방식으로 동작하는 것은 아니다 물론 트랜잭션의 기본 개념인 더이상 쪼갤수 없는 최소단위의 작업이라는 개념은 항상 유효하다.

따라서 트랜잭션 경계 안에서 진행된 작업은 commit () 을 통해 모두 성공하던지  아니면 rollback() 을 통해 모두 취소되어야한다.

그런데 이밖에도 트랜잭션의 동작방식을 제어 할 수 있는 몇가지 조건이있다.

## **트랜잭션 전파**

트랜잭션의 경계에서 이미 진행중인 트랜잭션이 있을 떄 또는 없을때 어떻게 동작 할것인가를 결정하는 방식을 말한다.

트랜잭션 전파와 같이 각각 독립적인 트랜잭션 경계를 가진 두개의 코드가 있다고 하자 . 그런데 a의 트랜잭션이 시작되고 아직 끝나지 않은 시점에서 b를 호출 했다면 b의 코드는 어떤 트랜 잭션 안에서 동작해야 할까?

이렇게 독자적인 트랜잭션 경계를 가진 코드에 대해 이미 진행 중인 트랜잭션이 어떻게 영향을 미칠 수 있는가를 정의 하는것이 트랜잭션  전파 속성이다.

대표적으로 다음과 같은 트랜잭션 전파 속성을 줄 수있다.

- PROPAGATION_REQULRED
    
    가장 많이 사용되는 트랜잭션 전파 속성,  진행중인 트랜잭션이 없으면 새로 시작하고 , 이미 시작된 트랜잭션이 있으면 이에 참여한다.
    
- PROPAGATION_REQUIRES_NEW
    
    항상 새로운 트랜잭션을 시작한다. 즉 앞에서 시작된 트랜잭션이 있든 없든 상관없이 새로운 트랜잭션을 만들어서 독자적으로 동작하게 한다.
    
- PROPAGATION_NOT_SUPPPORTED
    
    이 속성을 사용하면 트랜잭션 없이 동작 하도록 만들 수도 있다.
    
    그냥 트랜잭션 경계설정을 하지 않으면 되는게 아닐까 할수 있지만 , 트랜잭션 경계 설정은 보통 AOP를 이용해 한번에 많은 메소드에 동시에 적용하는 방법을 사용한다. 그런데 구중에서 특별한 메소드만 트랜잭션 적용에서 제외하려면 어떻게 해야할까? 모든 메소드에 트랜잭션 AOP가 적용 되게하고, 특정 메소드의 트랜잭션 전파 속성만 설정해서 트랜잭션 없이 동작하게 만드는 편이 낫다.
    

### **격리수준**

모든 트랜잭션은 격리수준을 갖고 있어야한다. 서버환경에서 여러개의 트랜잭션이 동시에 진행될 수 있다 . 가능하다면 모든 트랜잭션이 순차적으로 진행돼서 다른 트랜잭션의 작업에 독립적인 것이 좋겠지만, 그러자면 성능이 크게 떨어질 수 밖에없다. 따라서 적절하게 격리 수준을 조정해서 가능한 한 많은 트랜잭션을 동시에 진행 시키면서도 문제가 발생하지 않게 제어가 필요하다.

격리 수준은기본적으로 DB에 설정되어 있지만 JDBC 드라이버나 DataSource 등에서 재설정 할 수 있고, 필요하다면 트랜잭션 단위로 격리 수준을 조정 할 수 있다.

기본적으로는 DB나 DataSource에 설정된 디폴트 격리 수준을 따르는것이 좋지만 특별한 작업을 수행하는 메소드의 경우는 독자적인 격리 수준을 지정할 필요가 있다.

### **제한시간**

트랜잭션을 수행하는 제한시간을 설정 할수 있다 . DefaultTransactionDefinition의 기본설정은 제한시간이 없는것이다.

### **읽기 전용**

읽기 전용으로 설정해두면 트랜잭션 내에서 데이터를 조작하는 시도를 막아줄수 있다 . 또한 데이터 액세스 기술에 따라서 성능이 향상 될 수도있따.

트랜잭션의 정의를 수정하려면 어떻게 해야 할까? 외부에서 정의된 TransactionDefinition 오브젝트를 DI받아서 사용하도록 만들면된다.

## **트랜잭션 인터셉터와 트랜잭션 속성**

메소드별로 다른 트랜잭션 정의를 적용하려면 어드바이스의 기능을 확장해야한다 . 마치 초기에 TransactionHandler 에서 메소드 이름을 이용해 트랜잭션 적용 여부를 판단 했던 것과 비슷한 방식을 사용하면 된다. 메소드 이름 패턴에 따라 다른 트랜잭션 정의가 적용되도록 만드는것이다.

### **Transactioninterceptor**

이를위해 기존에 만들었던 TransactionAdvice를 다시 설계할 필요는 없다 . 이미 스프링에는 편리하게 트랜잭션 경계설정 어드바이스로 사용 할 수 있도록 만들어진 TrancactionInterceptor가 존재하기 떄문이다.

```
public Object invoke(MethodInvocation invocation)throws Throwable{
    TransactionStatus status =
        this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        try{
            Object ret = invocation.proceed();
            this.transactionManager.commit(status);
            return ret;
        }catch(RuntimeException e){
            this.transactionManager.rollback(status);
            throw e;
        }
}
```

TransactionAdvice는 RuntimeException 이 발생하는 경우에만  트랜잭션을 롤백 시킨다 . 하지만 런타임 예외가 아닌 경우에는 트랜잭션이 제대로 처리되지 않고 메소드를 빠져나가게 되어 있다 .

스프링이 제공하는 Transactionlnterceptor에는 기본적으로 두 가지 종류의 예외 처리 방식이 있다. 런타임 예외가 발생하면 트랜잭션은 롤백된다, 체크예외를 던지는 경우에는 이것을 예외 상황으로 해석하지 않고 일종의 비즈니스 로직에 따른 , 의미가 있는 리턴 방식의 한 가지로 인식해서 트랜잭션을 커밋해버린다.

그런데 ransactionlnterceptor의 이러한 예외처리 기본 원칙을 따르지 않는 경우가 있을 수도 있다 .

**그래서 TransactionAttribute는 rollbackOn()이라는 속성을 둬서 기본 원칙과 다른 예외 처리가 가능하게 해준다. 이를 활용하면 특정 체크 예외의 경우는 트랜잭션을 롤백시키고 , 특정 런타임 예외에 대해서는 트랜잭션을 커밋시킬수도 있다.**

### **메소드 이름 패턴을 이용한 트랜잭션 속성 지정**

Properties타입의 transactionAttributes 프로퍼티는 메소드 패턴과 트랜잭션 속성을 키와 값으로 갖는 컬렉션이다. 이중에서 트랜잭션 전파 항목만 필수이고 나머지는 다 생략 가능하다 . 생략하면 모두 DefaultTransactionDefinition에 설정된 디폴트 속성이 부여된다.

> 메소드 이름 패턴과 문자열로 된 트랜잭션 속성을 이용해서 정의한 TransactionInterceptor 타입 빈의 예다.
> 

```xml
<bean id="transactionAdvice"
class="org.springframework.transaction.interceptor.Transactionlnterceptor">
    <property name="transactionManager" ref="transactionManager" />
    <property name="transactionAttributes">
        <props>
            <prop key="get*">PROPAGATION_REQUIRED,readOnly , timeout_30</prop>
            <prop key=“upgrade*">PROPAGATION_REQUIRES_NEW, ISOLATION_SERIALIZABLE
            </prop>
            <prop key="*">PROPAGATION_REQUIRED</prop>
        </props>
    </property>
</bean>
```

> spring boot configuration  설정 방법
> 
> 
> [참고 블로그](https://linked2ev.github.io/gitlog/2019/10/02/springboot-mvc-15-%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-MVC-Transaction-%EC%84%A4%EC%A0%95/)
> 

```java
@Contiguration
public class TransactionAspect{
    @Autowired
    platformTransactionManager transactionManager;

    @Bean
    public TransactionInterceptor transactionAdvice() {
        NameMatchTransactionAttributeSource txAttributeSource = new NameMatchTransactionAttributeSource();
        RuleBasedTransactionAttribute txAttribute = new RuleBasedTransactionAttribute();
        //지정된 예외가 양수 및 음수 모두에 대해 여러 롤백 규칙을 적용하여 트랜잭션 롤백을 유발해야 하는지 여부를 확인하는 TransactionAttribute 구현입니다. 사용자 지정 롤백 규칙이 적용되지 않는 경우 이 특성은 DefaultTransactionAttribute(런타임 예외 시 롤백)처럼 작동

        txAttribute.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
        txAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        HashMap<String, TransactionAttribute> txMethods = new HashMap<String, TransactionAttribute>();
        txMethods.put("*", txAttribute);
        txAttributeSource.setNameMap(txMethods);

        return new TransactionInterceptor(transactionManager, txAttributeSource);
    }
```

## 포인트컷과 트랜잭션 속성의 적용 전략

트랜잭션 부가기능을 적용할 후보 메소드를 선정하는 작업은 포인트컷에 의해 진행된다 . 그리고 어드바이스의 트랜잭션 전파 속성에 따라서 메소드 별로 트랜잭션의 적용 방식이 결정된다 .

포인트컷 표현식과 트랜잭션 속성을 정의 할 때 따르면 좋은 몇가지 전략을 생각해보자.

- 트랜잭션 포인트컷 표현식은 타입 패턴이나 빈 이름을 이용한다.
- 공통된 메소드 이름 규칙을 통해 최소한의 트랜잭션 어드바이스와 속성을 정의한다
    
    가장 간단한 트랜잭션 속성 부여 방법은 다음과 같이 모든 메소드에 대해 디폴트 속성을 지정하는것이다 .개발이 진행됨에 따라 단계적으로 속성을 추가해주면된다.
    
    트랜잭션 적용 대상 클래스의 메소드는 일정한 명명 규칙을 따르게 해야한다.
    
    일반화하기에는 적당하지 않은 특별한 트랜잭션 속성이 필요한 타깃 오브젝트에 대해서는 별도의 어드바이스와 포인트컷 표현식을 사용하는편이 좋다.
    
- 프록시 방식 aop는 같은 타깃 오브젝트 내의 메소드를 호출 할 때는 적용되지 않는다.
    
    프록시 방식의 aop에서는 프록시를 통한 부가 기능의 적용은 클라이언트로부터 호출이 일어 날때만 가능하다. 여기서 클라이언트는 인터페이스를 통해 타깃 오브젝트를 사용하는 다른 모든 오브젝트들을 말한다. 반대로 타깃 오브젝트가 자기 자신의 메소드를 호출 할 때는 프록시를 통한 부가기능의 적용이 일어나지 않는다 . 프록시가 적용되는 방식을 생각해보면 왜 그런지 알 수 있을것이다.
    

## 어노테이션 트랜잭션 속성과 포인트 컷

클래스나 메소드에 따라 제각각 속성이 다른 세밀하게 튜닝된 트랜잭션 속성을 적용해야 하는 경우도 있다 이런 경우라면 메소드 이름 패턴을 이용해서 일괄적으로 트랜잭션 속성을 부여하는 방식은 적합하지 않다. 설정 파일에서 패턴으로 분류 가능한 그룹을 만들어서 일괄적으로 속성을 부여하는 대신에 직접 타깃에 트랜잭션 속성정보를 가진 애노테이션을 지정하는 방법이다.

### Transactional

```java
@Target({ElemntType.METHOD,ElementType.TYPE})
//어노페이션을 사용할 대상을 지정한다 . 여기에 사용된 "메소드와 타입(클래스,인터페이스)처럼 한개
//이상의 대상을 지정할수 있다.
@Retention(RetentionPolicy.RUNTIME)
//어노테이션 정보가 언제까지 유지되는지를 지정한다.
@Inherited
//상속을 통해서도 어노테이션 정보를 얻을수 있게한다.
@Documnetde
public @interface Transactional{
	//트랜잭션 속성의 모든 항목을 앨리먼트로 지정할수있다.
	//디폴트 값이 서정되어 있으므로 모두 생략 가능하다.
}
```

@Transactional 어노테이션의 타깃은 메소드와 타입이다 . 따라서 메소드 클래스 , 인터페이스에 사용 할수 있다. @Transactional  어노테이션을 트랜잭션 속성정보로 사용하도록 지정하면 @Transactional 이 부여된 모든 오브젝트를 자동으로 타깃 오브젝트로 인식한다. @Transactional 은 기본적으로 트랜잭션 속성을 정의하는 것이지만 , 동시에 포인트컷의 자동등록에도 사용된다.

### 트랜잭션 속성을 이용하는 포인트컷

@Transactional은 메소드 마다 다르게 설정 할수도 있으므로 매우 유연한 트랜잭션 속성 설정이 가능해진다 . 동시에 포인트컷도 @Transactional 을 통한 트랜잭션 속성정보를 참조 하도록 만든다.

@Transactional 로 트랜잭션 속성이 부여된 오브젝트라면 포인트컷의 선정대상이기도 하기 때문이다.

트랜잭션 속성은 타입 레벨에 일괄적으로 부여할 수 도있지만 , 메소드단위로 세분화해서 트랜잭션 속성을 다르게 지정할 수도 있기 때문에 매우 세밀한 트랜잭션 속성 제어가 가능해진다.

트랜잭션 부가기능 적용 단위는 메소드다. 따라서 메소드마다 @Transactional 을 부여하고 속성을 지정 할 수 있다 . 이렇게하면 유연한 속성제어는 가능 하겠지만 코드는 지저분해지고 , 동일한 속성 정보를 가진 애노테이션을 반복적으로 메소드마다 부여 해주는 바람직 하지 못한 결과를 가져올수있다.

### 대체 정책

그래서 스프링은 @Transactional을 적용할때 4 단계의 대체 정책을 이용하게 해준다.

순서에 따라서 @Transactional 이 적용됐는지 차례로 확인하고 , 가장 먼저 발견되는 속성 정보를 사용하게 하는 방법이다.

```java
[1]
public interface Service{
[2]
	void method1();
[3]
	void method2();
}
[4]
public class ServiceImpl implements Service{
[5]
	void method1(){
	}
[6]
	void method2(){
	}
}
```

스프링은 트랜잭션 기능이 부여될 위치인 타깃 오브젝트의 메소드 부터 시작해서 @Transactional 어노테이션이 존재하는지 확인한다 . 따라서 5 와 6이 @Transactional이 위치 할수있는 첫번째 후보다.

여기서 어노테이션이 발견되면 바로 어노테이션의 속성을 가져다 해당 메소드의 트랜잭션 속성으로 사용한다. 다음은 타깃 클래스인 4에 @Transactional이 존재하는지 확인한다 . 이를 통해서 @Transactional이 타입 레벨, 즉 클래스에 부여되면 해당 클래스의 모든 메소드의 공통적으로 적용되는 속성이 될수있다 . 타깃 클래스에서도 @Transactional을 발견하지 못하면 스프링은 메소드가 선언된 인터페이스로 넘어간다. 인터페이스에서도 먼저 메소드를 확인한다.

## 선언적 트랜잭션과 전파방식 + 표정리

선언적 트랜잭션이란 Trransaction Template와 달리 트랜잭션 처리를 코드에서 직접적으로 수행하지 않고 , 설정파일이나 어노테이션을 이용하여 트랜잭션의 범위나 롤백규칙등을 정의하는 것을 의미한다.

### 전파방식

호출되는 메소드와 클라이언트에 대하며 트랜잭션 경계를 정의

| 전파방식 | 의미 |
| --- | --- |
| NESTED | 이미 진행중인 트랜잭션이 있으면 중첩 트랜잭션을 시작한다 .( 중첩된 트랜잭션은 본 트랜잭션과 독립적으로 커밋되거나 롤백 될수 있다 .) |
| REQUIRES_NEW | 진행중인 트랜잭션이 있으면 잠시 보류시키고 항상 새로운 트랜잭션을 시작한다. |
| REQUIRED | 기본속성 , 미리 시작된 트랜잭션이 있으면 참여하고 , 아니면 시작한다. |

### @Transactional 어노테이션 속성

| 속성 | 설명 |
| --- | --- |
| propagation | 트랜잭션 전파 규칙 설정 . |
| isolation | 트랜잭션 격리 레벨 설정  |
| readOnly | 읽기 전용 여부 설정 |
| rollbackFor | 트랜잭션을 롤백할 예외 타입 설정 |
| noRollbackFor | 트랜잭션을 롤백하지 않을 예외 타입 설정  |
| timeout | 트랜잭션의 타임 아웃을 초단위로 설정  |

### 트랜잭션 격리 수준이란

트랜잭션 격리수준(isolation level)이란 동시에 여러 트랜잭션이 처리될 때, 트랜잭션끼리 얼마나 서로 고립되어 있는지를 나타내는 것이다.

즉, 간단하게 말해 특정 트랜잭션이 다른 트랜잭션에 변경한 데이터를 볼 수 있도록 허용할지 말지를 결정하는 것이다.

일반적인 온라인 서비스에서는 READ COMMITTED(**어떤 트랜잭션의 변경 내용이 COMMIT 되어야만 다른 트랜잭션에서 조회할 수 있다**.)나 REPEATABLE READ(**트랜잭션이 시작되기 전에 커밋된 내용에 대해서만 조회할 수 있는 격리수준**)중 하나를 사용