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





