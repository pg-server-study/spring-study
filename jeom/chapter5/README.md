# 5장 서비스 추상화

5장에서는 지금까지 만든 DAO에 트랜잭션을 적용해 보면서 스프링이 어떻게 성격이 비슷한 여러 종류의 기술을 추상화하고 이를 일관된 방법으로 사용할 수 있도록 지원하는지를 살펴볼것이다.

## 5.1 사용자 레벨 관리 기능 추가

이전까지 만들었던 UserDao는 User 사용자 정보를 DB에 넣고 빼는 것을 제외하면 어떤 비즈니스 로직도 갖고 있지않다. 이제 여기에 간단한 비즈니스 로직을 추가해 보자.

> 사용자 관리의 기본 로직은 정해진 조건에 따라 사용자의 레벨을 주기적으로 변경한다는 간단한 내용이다.
>
> 먼저 User 클래스에 사용자의 레벨을 저장할 필드를 추가하자 . 상수 값을 정해놓고 int 타입으로 레벨을 사용한다고 해보자.

```java
class User{
	private static final int BASLC = 1;
	private static final int SILVER = 2;
	private static final int GOLD = 3;
    
    int level;
    
    public void setLevel(int level){
        this,level = level;
    }
}
```

> DB에 저장될 때는 getLevel()이 돌려주는 숫자 값을 사용하면된다.

```java
if (user.getLevel() == User.BASLC){
	user1.setLevel(User.SILVER);
}
```

위에 코드가 문제 없이 돌아가는 것처럼 보이겠지만 사실은 레벨이 엉뚱하게 바뀌는 심각한 버그가 만들어진다.

`user1.setLevel(other.getSum());` 처럼 레벨이 엉뚱하게 바뀌거나 `user2.setLevel(1000);` 와 같이 범위를 벗어나는 값을 넣을 위험도 있다.

**그래서 숫자 타입을 직접 사용하는 것 보다는  자바 5 이상에서 제공하는 이넘(enum)을 이용하는게 안전하고 편리하다.**

```java
public enum Level{
	BASLC(1), SILVER(2),GOLD(3);
	
	private final int value;
	
	Level(int value){
		this.value = vlaue;
	}
	
	public int intValue(){
		return value;
	}
	
	public static Level valueOf(int value){
		switch(value){
			case 1: return BASIC;
			case 2: return SILVER;
			case 3: return GOLD;
			default: throws new AssertionError("Unknown value: "+ vlaue);
		}
	}
}
```

이렇게 만들어진 Level 이넘은 내부에는 DB에 저장할 int 타입의 값을 갖고 있지만 , 겉으로는 Level 타입의 오브젝트 이기 때문에 안전하게 사용할수 있다. 만일 `user2.setLevel(1000);`같은 코드는 컴파일러가 타입이 일치하지 않는다는 에러를 내면서 걸러줄것이다.

### User 필드에 추가 

```java
public class User{
	...
	Level level;
	int login;
	int recommend;
	
	public Level getLevel(){
		return level;
	}
	
	public void setLevel(Level level){
		this.level = level;
	}
	...
	//login , recommend getter/setter 생략
}
```

### UserDaoTest 테스트 수정

> UserDaoJdbc와 테스트에도 필드를 추가해야한다.

```java
public class UserDaoTest{
	...
	@Before
	public void setUp(){
		this.user1 = new User("gyumee","박성철","springno1",Level.BASIC,1,0);
		this.user2 = new User("leegw","이길원","springno2",Level.SIVER,55,10);
		this.user3 = new User("bumjin","박범진","springno3",Level.GOLD,100,40);
	}
}
```

> 추가된 필드를 파라미터로 포함하는 생성자

```java
class User{
	...
	public User(String id, String name , String password, Level level.
			int login, int recommend){
		this.id = id;
		this.name = name;
		this.password = passwrod;
		this.level = level;
		this.login = login;
		this.recomment = recomment;
	}
}
```

### UserDaoJdbc 수정

```java
public clas UserDaoJdbc implement UserDao{
	...
	private RowMapper<User> userMapper = 
		new RowMapper<User>(){
			public User mapRow(ResultSer re,int rowNum) throws SQLException{
				User user = new User();
				user.setId(rs.getString("id"));
				user.setPassword(rs.getString("password"));
				user.setLevel(Level.valueOf(rs.getInt("level")));
				user.setLogin(rs.getInt("login"));
				user.setRecommend(rs.getInt("recommend"));
				return user;
			}
		};
		
		public void add(User user){
			this.jdbcTemplate.update(
				"insert into users(id,name,password,level,login,recommend)"
				"values(?,?,?,?,?,?)",user.getId(), user.getName(),
				user.getPassword(), user.getLevel().intValue(),
				user.getLogin(),user.getRecommend());			
		}
}
```

### 사용자 수정 기능 추가 

기본키인 id를 제외한 나머지 필드는 수정될 가능성이 있다 . 수정할 정보가 담긴 user 오브젝트를 전달하면 id를 참고해서 사용자를 찾아 필드 정보를 update문을 이용해 모두 변경해주는 메소드를 하나 만들겠다.

> 사용자 정보 수정 메소드 테스트

```java
@Test
public void update(){
	dao.deleteAll();
	
	dao.add(user1);
	
	user1.setName("오민규");
	user1.setPassword("springno6");
	user1.setLevel(Level.GOLD);
	user1.setLogin(1000);
	user1.setRecommend(999);
	dao.update(user1);
	
	User user1update = dao.get(user1.getId());
	checkDameUser(user1,user1update);
}
```

먼저 픽스처 오브젝트를 하나 등록한뒤 id를 제외한 필드의 내용을 바꾸고 update()를 호출한다 . 그리고 다시 id로 조회해서 가져온 User 오브젝트와 수정한 픽스처 오브젝트를 비교한다.

### UserDao 와 UserDaoJdbc 수정

> UserDao인터페이스에 update() 메소드가 없기 때문에 컴파일 에러가 날것이다 . 인터페이스에 update 메소드를 추가해주자.

```java
pubic interface UserDao(){
	...
	public void update(User user);
}
```

> UserDao 인터페이스에 update()를 추가하고 나면 이번엔 UserDaoJdbc 에서 메소드를 구현하지 않았다고 에러가 날것이다 . UserDaoJdbc 의 update() 메소드를 add()와 비슷한 방식으로 만들면된다.

```java
public void update(User user){
	this.jdbcTemplate.update(
		"update users set name = ?, password = ?, level = ?, login = ?," +
			"recommend = ? where id = ?", user.getName(), user.getPassword(),
			user.getLevel().intValue(),user.getLogin(),user.getRecommend(),
			user.getId());
}
```

### 수정 테스트 보완

상단에 사용자 정보 수정 메소드 테스트로는 검증하지 못하는 오류가 있을수 있다 . 바로 update 문장에서 where 절을 빼먹는 경우다. update는 where가 없어도 아무런 경고 없이 정상적으로 동작하는것 처럼 보인다.

그리고 수정하지 않아야 할 로우의 내용이 그대로 남아있는지는 확인해주지 못한다는 문제가 있다. 이문제를 해결할 방법을 생각해보자.

첫번째 방법은 JdbcTemplate의 update()가 돌려주는 리턴값을 확인하는것이다.

두번째 방법은 테스트를 보강해서 원하는 사용자 외의 정보는 변경되지 않았음을 직접 확인하는 것이다.

확실하게 테스트 하려면 UserDao update()메소드의 SQL 문장에서 where 부분을 빼보면된다 . 그래도 기존 update() 테스트는 성공할 것이다. 테스트에 결함이 있다는 증거다 . 이상태에서 테스트를 수정해서 테스트가 실패하도록 만들어야한다.

> 두번째 방법으로 테스트를 보완한것이다.

```java
@Test
public void update(){
	dao.deleteAll();
	
	dao.add(user1); //수정할 사용자
	dao.add(user2);	//수정하지 않을 사용자
	
	user1.setName("오민규");
	user1.setPassword("springno6");
	user1.setLevel(Level.GOLD);
	user1.setLogin(1000);
	user1.setRecommend(999);
	
	dao.update(user1);
	
	User user1update = dao.get(user1.getId());
	checkSameUser(user1,user1update);
	User user2same = dao.get(user2.getId));
	checkSameUser(user2,user2same);
}
```

update() 메소드에서 SQL에서 WHERE를 빼먹었다면 이테스트는 실패로 끝날 것이다. 이제 테스트가 성공하도록 SQL을 원래대로 만들어주면 된다.

### 5.1.3 UserService.upgradeLevels()

사용자 관리 비즈니스 로직을 담을 클래스를 하나 추가하자 . 비즈니스 로직 서비스를 제공한다는 의미에서 클래스 이름은 UserService 로 한다 .

### UserService 클래스와 빈등록

UserService 클래스를 만들고 사용할 UserDao 오브젝트를 저장해둘 인스턴스변수를 선언한다 . 오브젝트의 DI가 가능하도록 수정자 메소드도 추가한다.

```java
public class UserService{
	UserDao userDao;
	
	public void setUserDao(User userDao){
		this.usrDao = userDao;
	}
}
```

그리고 스프링 설정파일에 userService 아이디로 빈을 추가한다 . userDao빈을 DI받도록 프로퍼티를 추가해준다.

```java
<bean id="userService" class="springbook.user.service.UserService">
	<property name="userDao" ref="userDao" / >
</bean>

<bean id="userDao" class="springbook.dao.UserDaoJdbc">
	<property name="dataSource" ref="dataSource" />
</bean>
```

### UserServiceTset 테스트 클래스 

다음은 UserServiceTest 클래스를 추가하고 테스트 대상인 UserService빈을 제공 받을수 있도록 @Autowired가 붙은 인스턴스 변수로 선언해준다.

```java
@RunWith(SpringJunit4ClassRunner.class)
@ContextConfiguration(locations="/tset-applicationContext.xml")
public class UserServiceTset{
	@Autowired
	UserService userService;
    
    @Test
    public void bean(){ //메소드가 없으면 에러가 나오기때문에 빈이 생성되서 변수에 주입되는지만 확인하는 메소드 추가
        	assertThat(this.userService,is(notNullValue())); 
    }
}
```

### upgradeLevels() 메소드

> 이번엔 사용자 레벨 관리 기능을 먼저 만들고 테스트를 만들어보자.

```java
public void upgradeLevels(){
	List<User> users = userDao.getAll();
	for(User:users){
		Boolean changed = null; //레벨의 변화가 있는지를 확인하는 플래그
		if(user.getLevel() == Level.BASIC && user.getLogin() >= 50){
			user.setLevel(Level.SILVER); //BASIC 레벨 업그레이드 작업
			changed = true;
		}
        else if(user.getLevel() == level.SILVER && user.getRecommend() >= 30){
        	user.setLevel(Level.GOLD); //SILVER 레벨 업그레이드 작업
        	changed = true;
        }
        else if (user.getLevel() == Level.GOLD){ changed = false; } //GOLD레벨은 변경 x
        else { changed = false; } //조건 없으면 변경 없음
        if(changde){ userDao.update(user); } // 변경이 있는경우에만 update 호출
	}
}
```

### upgradeLevels() 테스트

```java
class UserServiceTest{
	...
	List<User> users; //테스트 픽스처 
	
	@Before
	public void setUp(){
		users = Arrats.asList( //배열을 리스트로 만들어주는 메소드 
				new User("bumjin","박범진","p1",Level.BASIC,49,0),
				new User("joytouch","강명성","p2",Level.BASIC,50,0),
				new User("erwins","신승한","p3",Level.SILVER,60,29),
				new User("madnite1","이상호","p4",Level.SILVER,60,30),
				new User("green","오민규","p5",Level.GOLD,100,100),
		);
	}
}
```

> 준비된 픽스처를 사용해 만든 테스트 

```java
@Test
public void upgradeLevels(){
	userDao.deleteAll();
	for(User:users) userDao.add(user);
	
	userService.upgradeLevels();
	
	checkLevel(users.get(0),Level.BASIC);
	checkLevel(users.get(1),Level.SILVER);
	checkLevel(users.get(2),Level.SILVER);
	checkLevel(users.get(3),Level.GOLD);
	checkLevel(users.get(4),Level.GOLD);
    //각 사용자 별로 업그레이드 후의 예상레벨을 검증한다 .
}

private void checkLevel(User user,Level expectedLevel){
	User userUpdate = userDao.get(user.getId());
	assertThat(UserUpdate.getLevel(),is(expectedLevel));
}
```

### 5.1.4 UserService.add()

사용자 관리 비즈니스 로직에서 대부분은 구현 했지만 처음 가입하는 사용자는 기본적으로 BASIC 레벨이여야한다는 부분을 구현하지 않았다 .

UserService 에 add()를 만들어두고 사용자가 등록 될때 적용할만한 비즈니스 로직을 담당하게한다.

```java
@Test
public void add(){
	userDao.deleteAll();
	
	User userWithLevel = users.get(4); //이미 레벨이 gold일 경우 레벨을 초기화 하지 않는다.
	User userWithoutLevel = users.get(0); // 레벨이 비어있는 사용자 
	userWithoutLevel.setLevel(null);
	
	userService.add(userWithLevel);
	userService.add(userWithoutLevel);
	
	User userWithLevelRead = userDao.get(userWhitLevel.getId());
	User userWihtoutLevelRead = userDao.get(userWithoutLevel.getId());
    //DB에 저장된 결과를 가져와 확인
	
	assertThat(userWithLevelRead.getLevel(),is(userWithLevel.getLevel()));
	assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
}
```

> 사용자 신규 등록 로직을 담은 add() 메소드

```java
public void add(User user){
	if(user.getLevel() == null) user.setLevel(Level.BASIC);
	userDao.add(user);
} 
```

### upgradeLevels() 리팩토링

> 읽기 불편했던 for 속에 if / else if / else 문들을 분리해 기본작업 흐름만 남겨두었다.

```java
public void upgradeLevels(){
	List<User> users = userDao.getAll();
	for(User user: users){
		if(canUpgrandeLevel(user)){
			upgradeLevel(user);
		}
	}
}
```

```java
private boolean canUparadeLevel(User user){
	Level currentLevel = user.getLevel();
	switch(currentLevel){
		case BASIC: return (user.getLogin()>=50); //레벨별로 구분해서 조건을 판단
		case SILVER: return (user.getRecommend()>=30);
		case GOLD: return false;
		default: throw new IllegalArgumentException("Unknown Level:" + currentLevel);
        //다룰수 없는 레벨이 주어지면 레벨 발생
	}
}
```

```java
private void upgradeLevel(User user){
	if(user.getLevel() == Level.BASIC) user.setLevel(Level.SILVER);
	else if(user.getLevel() == Level.SILVER) user.setLevel(Level.GOLD);
	userDao.update(user);
}
```

다음레벨이 무엇인가하는 로직과 오브젝트의 level 필드를 변경해 준다는 로직이 함께 있는데다 , 너무 노골적으로 드러나 있다 . 게다가 예외 상황에 대한처리가 없다. 더군다나 레벨이 늘어난다면 if 문이 점점 길어질 것이다.

```java
public enum Level{
	GOLD(3,null), SILVER(2,GOLD),BASIC(1,SILVER);
	//이넘 선언에 DB에 저장할 값과 함께 다음 단계의 레벨정보도 추가한다.
	private final int value;
	private final Level next; //다음 단계의 레벨 정보를 스스로 갖고 있도록 추가
	
	Level(int vlaue,Level next){
		this.value = value;
		this.next = next;
	}
	
	public int intValue(){
		return value;
	}
	
	public Level nextLevel(){
		return this.next;
	}
	
	public static Level vlaueOf(int value){
		switch(value){
			case 1: return BASIC;
			case 2: return SLCVER;
			case 3: retrun GOLD;
			default: throws new AssertionError("Unknown vlaue:"+value);
		}
	}
}
```

```java
public void upgradeLevel(){
	Level nextLevel = this.level.nextLevel();
	if(nextLevel == null){
		throws new IllegalStateException(this.level+"은 업그레이드가 불가능합니다.")
	}
	else{
		this.level = nextLevel;
	}
}
```

> 간결해진  UserService 의 upgradeLevel

```java
private void upgradeLevel(User user){
	user.upgradeLevel();
	userDao.update(user);
}
```

### User 테스트

```java
public class UserTest{
	User user;
	
	@Before
	public void setUp(){
		user = new User();
	}
	
	@Test()
	public void upgradeLevel(){
		Level[] levels = Level.values();
		for(Level level : levels){
			if(level.nextLevel() == null) continue;
			user.setLevel(level);
			user.upgradeLevel();
			assertThat(user.getLevel(),is(level.nextLevel()));
		}
	}
	
	@Test(expected = IloegalStateException.class)
	public void cannotUpgradeLevel(){
		Level[] levels = Level.vlaues();
		for(Level level:levels){
			if(level.nextLevel()!=null) continue;
			user.setLevel(level);
			user.uparadeLevel();
		}
	}
}
```

## 트랜잭션 서비스 추상화

중간에 예외가 발생해서 작업이 중단된다면 어떻게 될까? 이미 변경된 사용자의레벨은 작업 이전 상태로 돌아갈까 ? 아니면 바뀐채로 남아있을까 ? 

테스트를 위한 가장 쉬운 방법은 예외를 강제로 발생 시키도록 애플리케이션 코드를 수정하는 것이다 . 하지만 테스트를 위해 코드를 함부러 건드리는 것은 좋은 생각이아니다.

> 먼저 테스트용으로 UserService 를 상속한 클래스를 하나 만든다 . UserService 메소드의 대부분은 현재 privete 접근제한이 걸려있어 오버라이딩이 불가능 하다 . 테스트 코드는 테스트 대상 클래스의 내부의 구현내용을 고려해서 밀접하게 접근해야 하는데 privete 처럼 제약이 강한 사용자를 사용하면 불편하다. 
>
> 테스트를 위해 애플리케이션 코드를 직접 수정하는 일은 가능한 피하는 것이 좋지만 이번에는 예외로 해야할것같다.
>
> 먼저 UserService 의 upgradeLevel() 메소드의 접근 권한을 다음과같이 수정하자 `protected void upgradeLevel(User user)` 이제 UserService 를 상속해서 UserService 대역을 맡을 클래스를  UserService Test에 추가한다.

```java
static class TestUserService extends UserService{
	private String id;
	
	private TestUserService(String id){ //예외를 발생시킬 user 오브젝트의 ID를 지정할수있게함
		this.id = id;
	}
	
	protected void upgradeLevel(User user){ //UserService 메소드를 오버라이드
		if(user.getId().equals(this.id))throws new TestServiceEcxeption();
        // 지정된 id의 User 오브젝트가 발견되면 예외를 던져서 작업중단시킴
		super.upgradeLevel(user);
	}
} 
```

### 강제 예외 발생을 통한 테스트

```java
@Test
public void upgradeAllOrNothing(){
	UserService testUserService = new TestService(users.get(3).getId());
    //예외를 발생시킬 네번째 사용자 id를 넣어서 테스트용 userservice 오브젝트 생성 
	testUserService.setUserDao(this.userDao); //수동 DI
	userDao.deldetAll();
	for(Uesr user : usres) userDao.add(user);
	
	try{ //예외가 발생하지 않는다면 실패 
		testUserService.upgradeLevels();
		fail("TestUserServiceException expected");
	}catch(TestUserServiceException e){	 //예외를 잡아서 계속 진행되도록 한다 . 
	}
	checkLevelUpgraded(user.get(1),false);
    //예외가 발생하기 전에 레벨 변경이 있었던 사용자의 레벨이 처음상태로 바뀌었나 확인.
}
```

우리가 기대 하는건 네번째 사용자를 처리하다가 예외가 발생해서 작업이 중단되었으니 **이미 레벨을 수정했던 두번째 사용자도 원래 상태로 돌아가는 것이다.** 하지만 위 테스트는 실패한다.

### 테스트 실패의 원인 

**바로 트랜잭션 문제다.** 모든 사용자의 레벨을 업그레이드 하는 작업인 upgraedeLevel() 메소드가 하나의 트랜잭션 안에서 동작하지 않았기 때문이다 . 

**트랜잭션이란 더 이상 나눌수 없는 단위 작업을 만한다. 작업을 쪼개서 작은 단위로 만들수 없다는 것은 트랜잭션의 핵심 속성인 원자성을 의미한다.**

모든 사용자에 대한 레벨 업그레이드 작업은 새로 추가된 기술 요구사항대로 전체가 다 성공하던지 아니면 전체가 다 실패하던지 해야한다 . 레벨 업그레이드 작업은 그 작업을 쪼개서 부분적으로는 성공하기도 하고, 여러번에 걸쳐서 진생할수 있는 작업이 아니여야한다. 

**작업을 완료할수 없다면 아예 작업이 시작되지 않은것 처럼 초기 상태로 돌려놔야 한다. 이것이 바로 트랜잭션이다.**

## 5.2.2 트랜잭션 경계설정

DB는 그 자체로 완벽한 트랜잭션을 지원한다 . SQL을 이용해 다중 로우의 수정이나 삭제를 위한 요청을 했을때 일부 로우만 삭제되고 나머지는 안된다거나 일부 필드는 수정 했는데 나머지 필드는 수정이 안되고 실패로 끝나는 경우는 없다 . **하나의 SQL 명령을 처리하는 경우는 DB가 트랜잭션을 보장해준다고 믿을수 있다. **

하지만 여러개의 SQL이 사용되는 작업을 하나의 트랜잭션으로 취급해야하는 경우도있다.

문제는 첫번째 SQL을 성공적으로 실행했지만 두번째 SQL이 성공하기 전에 장애가 생겨서 작업이 중단되는 경우다 . 이때 두가지 작업이 하나의 트랜잭션이 되려면 두번쨰 SQL이 성공적으로 DB에서 수행되기 전에 문제가 발생할 경우에는 앞에서 처리한 SQL 작업도 취소 시켜한다 . **이런 취소작업을 트랜잭션 롤백 이라고한다.**

반대로 여러개의 SQL을 하나의 트랜잭션으로 처리하는 경우에 모든 SQL 수행 작업이 다 성공적으로 마무리 됐다고 DB에 알려줘서 작업을 확정시켜야한다. **이것을 트랜잭션 커밋이라고 한다.**

### JDBC 트랜잭션의 트랜잭션 경계설정

모든 트랜잭션은 시작하는 지점과 끝나는 지점이 있다. 시작하는 방법은 한가지 이지만  끝나는 방법은 두가지다. **모든 작업을 무효화하는 롤백고 모든 작업을 다 확정하는 커밋이다.**

애플리케이션 내에서 트랜잭션이 시작되고 끝나는 위치를 트랜잭션의 경계라고 부른다 . 복잡한 로직 흐름 사이에서 정확하게 트랜잭션 경계를 설정하는 일은 매우 중요한 작업이다.

```java
Connection c = dataSource.getConnection();

c.setAutoCommit(false);
try{
	PrepardeStatement st1 =
		c.prepareStatement("update users ...");
	st1.executeUpdata();
	
	PrepardeStatement st2 =
		c.prepateStatement("delete users...");
	st2.executeUpdata();
   
	c.commit(); //트랜잭션 커밋 
    
}catch(Exception e){
	c.rollback() //트랜잭션 롤백
}
c.close();
```

Jdbc 기본 설정은 DB 작업을 수행한 직후에 자동으로 커밋이 되도록 되어있다 . ( 여러개의 DB 작업을 모아서 트랜잭션을 만드는 기능이 꺼져있다. 상단 코드에`c.setAutoCommit(false)` 로 자동 커밋 옵션을 false로 설정하였다 . )

트랜잭션이 한번 시작되면 commit() 또는 rollback 메소드가 호출될때까지의 작업이 하나의 트랜잭션으로 묶인다 .

이렇게 c.setAutoCommit(false) 로 트랜잭션의 시작을 선언하고 commit () 또는 rollback()로 **트랜잭션을 종료하는 작업을 트랜잭션의 경계설정이라고 한다.**

이렇게 하나의 DB 커넥션 안에서 만들어 지는 트랜잭션을 로컬 트랜잭션 이라고도한다.

### UserService 와 UserDao의 트랜잭션 문제 

그렇다면 왜 UserService의 upgradeLevels()에는 트랜잭션이 적용되지 않았을까 ? 

- 지금까지 만든코드 어디에도 트랜잭션을 시작하고 커밋, 롤백하는 설정 코드가 없다.
- 템플릿 메소드 호출 한번에 한개의 DB커넥션이 만들어지고 닫히는일이 일어난다 . **일반적으로 트랜잭션은 커넥션보다도 존재 범위가 짧다, **따라서 템플릿 메소드가 호출 될때마다 트랜잭션이 새로 만들어지고 메소드를 빠져나오기 전에 종료된다 . **결국 JdbcTemplate의 메소드를 사용하는 UserDao는 각 메소드마다 하나의 독립적인 트랜잭션으로 실행될 수 밖에없다**

UserDao는 JdbTemplate을 통해 매번 새로운 DB커넥션과 트랜잭션을 만들어 사용한다. 만일 UserDao의 update()를 세번에 걸쳐서 호출할경우 두번째 호출시점에서 오류가 발생해서 작업이 중단된다해도 매번 새로운 DB커넥션과 트랜잭션을 만들어 사용하기 때문에 첫번째 커밋한 트랜잭션의 결과는 DB에 그대로 남는다.

## 5.2.3 트랜잭션 동기화 

 스프링이 제안하는 방법은 독립적인 트랜잭션 동기화 방식이다 . 트랜잭션 동기화란 UserService에서 트랜잭션을 시작하기 위해 만든 Conneciton 오브젝트를 특별한 저장소에 보관해두고 , 이후에 호출되는 DAO 메소드에서는 저장된 Connection을 가져다가 사용하게하는것이다 . 정확히는 DAO가 사용하는 JdbcTemplate이 트랜잭션 동기화 방식을 이용하도록 하는것이다 . 그리고 트랜잭션이 모두 종료되면 그때는 동기화를 마치면 된다.

1. UserService 는 Connection을 생성하고 
2. 이를 트랜잭션 동기화 저장소에 저장 해두고 Conneciton의 setAutoCommit(false)를 호출해 트랜잭션을 시작 시킨후에 본격적으로 DAO의 기능을 이용하기 시작한다. 
3. 첫번째 update() 메소드가 호출되고 update()메소드 내부에서 이용하는 JdbcTemplate 메소드에서는 가장먼저 
4. 트랜잭션 동기화 저장소에 현재 시작된 트랜잭션을 가진 Connection 오브젝트가 존재하는지 확인한다 . (2) upgradeLevels() 메소드 시작 부분에서 저장해둔 Connection을 발견하고 이를 가져온다 .
5. Connection을 이용해 PreparedStatement를 만들어 수정 SQL을 실행한다. 트랜잭션 동기화 저장소에서 DB 커넥션을 가져왔을때는 JdbcTemplate은 Connection을 닫지 않은채로 작업을 마친다 . 

이렇게해서 트랜잭션 안에서 첫번째 DB 작업을 마쳤다 . **여전히 Connection은 열려있고 트랜잭션은 진행중인 채로 트랜잭션 동기화 저장소에 저장되어있다. **

트랜잭션 내에 모든 작업이 정상적으로 끝났다면 UserService는 이제 Connection의 commit()를 호출해서 트랜잭션을 완료 시킨다 . 마지막으로 트랜잭션 저장소가 더이상 Connetion 오브젝트를 저장해두지 않도록 이를 제거한다.

트랜잭션 동기화 저장소는 작업 스레드마다 독립적으로 Connection 오브젝트를 저장하고 관리하기 때문에 다중 사용자를 처리하는 서버의 멀티 스레드 환경에서도 충돌이날 염려는 없다.

### 트랜잭션 동기화 적용

```java
private DataSource dataSource;

public void setDateSource(DataSource dataSource){
	this.dataSource = dataSource;
}

public void upgradeLevels() throws Exception{
    TransactionSynchronizationManager.initSynchronization();
    //트랜잭션 동기화 관리자를 이용해 동기화 작업 초기화
    Connection c = DataSourceUtils.getConnection(dataSource);
    //커넥션을 생성하고 트랜잭션을 시작한다. 이후의 DAO 작업은 모두 여기서 시작한 
    //트랜잭션 안에서 진행된다 .
    c.setAutoCommit(false);
    
    try{
        List<User> users = userDao.getAll();
        for(User user:users){
            if(canUpgradeLevel(user)){
                upgradeLevel(user);
            }
        }
        c.commit();
    }catch(Exception e){ //예외 발생시 롤백
        c.rollback();
        throw e;
    }finally{
        DataSourceUtils.releaseConnection(c,dataSource);
        TransactionSynchronizationManager.unbindResource(this.dataSource);
        TransactionSynchronizationManager.clearSynchronization();
    }
}
```

	### 트랜잭션 테스트 보완

```java
@Autowried DataSource dataSource;

@Test
pulic void upgradeAllOrNothing() throws Exception{
	UserService testUserService = new TestUserService(users.get(3).getId());
	testUserService.setUserDao(this.userDao);
	testUserService.setDataSource(this.dataSource);
	...
}
```

### JdbcTemplate과 트랜잭션 동기화

jdbcTemplate은 만약 미리 생성 돼서 트랜잭션 동기화 저장소에 등록된 DB커넥션이나 트랜잭션이 없는 경우에는 JdbcTemplate이 직접 DB 커넥션을 만들고 트랜잭션을 시작해서 JDBC 작업을 진행한다 . 반면에 upgradeLevels () 메소드에서 처럼 트랜잭션 동기화를 시작해 놓았다면 그떄부터 실행되는 JdbcTemplate의 메소드에서는 직접 DB 커넥션을 만드는 대신 트랜잭션 동기화 저장소에 들어 있는 DB커넥션을 가져와서 사용한다.

### 기술과 환경에 종속되는 트랜잭션 경계 설정 코드

한개 이상의  DB로 작업을 하나의 트랜잭션으로 만드는건 Jdbc의 Counnection을 이용한 트랜잭션 방식인 로컬 트랜잭션으로는 불가능하다 . 로컬 트랜잭션은 하나의 DB Connection에 종속되기 때문이다.

각 DB 와 독립적으로 만들어지는 Connection을 통해서가 아니라 , 별도의 트랜잭션 관리자를 통해 트랜잭션을 관리하는 **글로벌 트랜잭션 방식을 사용해야한다.**

자바는 JDBC외에 이런 글로벌 트랜잭션을 지원하는 트랜잭션 매니저를 지원하기 위한 API인 JTA를 제공하고 있다.

트랜잭션은 JDBC나 JMS API를 사용해서 직접 제어하지 않고 JTA 통해 트랜잭션 매니저가 관리하도록 위임한다.

> JTA를 이용한 트랜잭션 처리코드의 전형적인 구조

```java
InitialContext ctx = new InitialContext();
UserTransaction tx = (UserTransaction)ctx.lookup(USER_TX_JNDI_NAME);
//JNDI를 이용해서 서버의 UserTransaction 오브젝트를 가져온다.
tx.begin();
Connection c = dataSource.getConnection(); //JNDI로 가져온 DataSource를 사용해야한다.

try{ //데이터 엑세스 코드
	tx.commit();
}catch(Exception e){
	tx.rollback();
}finally{
	c.close();
}
```

문제는 Jdbc 로컬 트랜잭션을 JTA를 이용하는 글로벌 트랜잭션으로 바꾸려면 UserService의 코드를 수정해야한다는 점이다.

로컬 트랜잭션을 사용하면 충분한 고객을 위해서는 jdbc를 이용한 트랜잭션 관리 코드를 , 다중 db를 위한 글로벌 트랜잭션을 필요로 하는 곳을 위해서는 JTA를 이용한 트랜잭션 관리 코드를 적용해야한다는 문제가 생긴다 . UserService 는 자신의 로직이 바뀌지 않았음에도 기술환경에 따라서 코드가 바뀌는 코드가 바뀌고 되버리고말았다.

### 트랜잭션 api의 의존관계 문제와 해결책

JDBC 트랜잭션 api와 JdbcTemplate과 동기화하는 api로 인해 JDBC DAO에 의존하게 된다. ( 다시 특정 데이터 액세스 기술에 종속되는 구조가 되어 버리고 말았다 . )

### 스프링의 트랜잭션 서비스 추상화

스프링은 트랜잭션 기술의 공통점을 담은 트랜잭션 추상화 기술을 제공하고 있다 . 이를 이용하면 애플리케이션에서 직접 각 기술의 트랜잭션 API를 이용하지 않고도 일관된 방식으로 트랜잭션을 제어하는 트랜잭션 경계 설정 작업이 가능해진다 .

> 스프링이 제공하는 트랜잭션 추상화 방법을 적용한 코드

```java
pulic void upgradeLevels(){
	PlatformTransactionManager transactionManager =
		new DataSourceTransactionManager(dataSource);
		
	TransactionStatus status =
		transactionManager.getTransaction(new DefaultTransactonDefinition());
    //트랜 잭션에 대한 속성을 담고있는 DefaultTransactonDefinition 오브젝트
	try{
		List<User> users = userDao.getAll();
		for(User user:users){
			if(canUparadeLevel(user)){
				upgradeLevel(user);
			}
		}
		transactionManger.commit(status);
	}catch(RuntimeException e){
		transactionManager.rollback(status);
		throw e;
	}
}
```

스프링이 제공하는 트랜잭션 경계설정을 위한 추상 인터페이스는 PlatformTransactionManager다. JDBC의 로컬 트랜잭션을 이용한다면 PlatformTransactionManager을 구현한 DataSourceTransactionManager를 사용하면 된다. 사용할 DB의 DataSource를 생성자 파라미터로 넣으면서 DataSourceTransactionManager의 오브젝트를 만든다.

### 트랜잭션 기술 설정의 분리 

트랜잭션 추상화 api를 적용한 UserService 코드를 JTA를 이용하는 글로벌 트랜잭션으로 변경하려면 어떻게 해야할까  ? PlatformTransactionManager 구현 클래스를 DataSourceTransactionManager에서 JTATransactionManger로 바꿔주기만 하면된다. `PlatformTransactionManager txManager = new JTATransactionManger();` 로 수정해주면된다. 

**하지만 어떤 트랜잭션 매니저 구현 클래스를 사용할지 UserService 코드가 알고있는 것은 DI원칙에 위배된다. ** 컨테이너를 통해 외부에서 제공 받게 하는 스프링의 DI 방식으로 바꾸자.

UserService에는 PlatformTransactinManager 인터페이스 타입의 인스턴스 변수를 선언하고 수정자 메소드를 추가해서 DI가 가능하게 해준다.

> PlatformTransactinManager를 빈으로 독립하고 DI받아서 사용하도록 수정한 UserSevice클래스 

```java
pulic class UserService {
	...
	private PlateformTransactionManager transactionManager;
	
	public void setTransactionManager(PlatformTransactionManager transactionManager){
		this.transactionManager = transationManger;
	}
	
	public void upgradeLevel(){
		TransactionStatus status = 
			this.transactionManager.getTransaction(new DefaultTransactionDefinition());
			try{
				List<User> users = userDao.getAll();
				for(User user:uesrs){
					if(canUparadeLevel(user)){
						upgradeLevel(user);
					}
				}
                this.transactionManager.commit(status);
			}catch(RuntimeException e){
                this.transactionManager.rollback(status);
                throw e;
            }
	}
    ...
}
```

```xml
<bean id="userService" class="springbook.user.service.UserService“>
	<property name="userDao" ref="userDao" /> 
	<property name="transactionManager" ref="transactionManager" /> 
</bean> 

<bean id="transactionManager" 
		class="org.springframework.idbc.datasource.DataSourceTransactionManager"> 
	<property name="dataSource" ref="dataSource" /> 
</bean>
```

DataSourceTransactionManager은 dataSource빈으로 부터 Connection 을 가져와 트랜잭션처리를 해야하기 때문에 dataSource 프로퍼티로 갖는다 . userService 빈도 기존의 dataSource프로퍼티를 없애고 새롭게 추가한 transactonManager 빈을 DI받도록 프로퍼티를 설정한다.

> 트랜잭션 매니저를 수동으로 DI하도록 수정한 테스트

```java
public class UserServiceTest{
	@Autowired
	PlatformTransactionManager transactionManager;
	
	@Test
	public void upgradeAllOrNothing() throws Exception{
		UserService testUserService = new TestUserService(user.get(3).getId());
		testUserService.setUserDao(userDao);
		testUserService.setTransactionManager(transactionManager);
		...
	}
}
```

> 만일 JTA를 이용하는 것으로 바꾸고 싶다면 설정 파일에서 다음과 같이 고치면된다.

```XML
<bean id="transactionManager" 
	class="org .springframework.transaction.jta.JtaTransactionManager" />
```

###  수직 , 수평 계층 구조와 의존관계

애플리케이션 로직 종류에 따른 수평적인 구분이든, 로직과 기술이라는 수직적인 구분이든 모두 결합도가 낮으며, 서로 영향을 주지 않고 자유롭게 확장될 수있는 구조를 만들수 있는데는 스프링의 DI가 중요한 역할을 하고있다 . DI의 가치는 이렇게 관심 , 책임, 성격이 다른 코드를 깔끔하게 분리하는데 있다.

### 단일 책임 원칙

이런 적절한 분리가 가져오는 특징은 객체 지향 설계의 원칙중 하나인 단일 책임 원칙으로 설명할수 있다 . 단일 책임 원칙은 하나의 모듈은 한가지 책임을 가져야한다는 의미이다 . 하나의 모듈이 바뀌는 이유는 한가지 여야한다고 설명 할수도 있다.

### 단일 책임 원칙의 장점

단일 책임원칙을 잘 지키고 있다면 , 어떤 변경이 필요할때 수정 대상이 명확해진다 . 기술이 바뀌면 기술 계층과의 연동을 담당하는 기술 추상화 계층의 설정만 바꿔주면 된다 .
