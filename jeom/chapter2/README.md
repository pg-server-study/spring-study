

# 2장 테스트 

테스트란 무엇이며 , 대표적인 테스트 프레임워크를 소개하고 , 이를 이용한 학습 전략을 알아볼것이다 .

## 목차

1.  UserDaoTset 살펴보기
   - 단위테스트란?
   - 자동수행 테스트코드
   - UserDaoTset 의 문제점
2.  UserDaoTset 개선
3.  Junit에 대해서 
   - 테스트 적용
   - 애플리케이션 컨텍스트 관리
4. 학습테스트

## UserDaoTset 살펴보기

> 1장에서 만들었던 main() 메소드로 작성된 테스트 코드이다. ( 앞으로 리팩토링하며 설명할예정 )
>
>  https://github.com/pg-server-study/springboot-toby spring boot 버전은 링크참조 -  JH님 감사 :) -

```java
public class UserDaoTest ( 
	public static void main(String[] args) throws SQLException ( 
		ApplicationContext context = new GenericXmlApplicationContext( 
			pplicationContext.xml"); 
            
		UserDao dao = context.getBean("userDao" , UserDao.class);
            
		User user = new User(); 
		user.setld("user"); 
		user.setName(" 백기선"); 
		user.setPassword( arried"); 
                         
		dao .add(user); 
                         
		System.out.println(user. getId() + " 등록 성공“); 
                           
		User user2 = dao .get(user.getld()); 
		System.out .println(user2 .getName()); 
		System.out.println(user2 .getPassword()); 
                           
		System.out .println(user2 .getld() + " 조회 성공");
      }
}
```

UserDaoTset는 main() 메소드를 이용해 add() , get () 메소드를 호출하고 , 

그결과를 화면에 출력해서 그값을 눈으로 확인 시켜준다 .

이렇게 만든 테스트용 main() 메소드를 반복적으로 실행해 가면서 처음 설계 한대로 기능이 동작하는지를

확인한 덕분에 다양한 방법으로 UserDao 코드의 설계와 코드를 개선 했고 , 스프링을 적용해서 동작하게 만들었다.

테스트란 결국 내가 예상하고 의도했던 대로 코드가 정확히 동작하는지를 확인해서 , 

만든 코드를 확신할 수 있게 해주는 작업이다.

### 단위테스트 

한꺼번에 너무 많은것을 몰아서 테스트 하면 테스트 수행과정도 복잡해지고 , 오류가 발생 했을 때 정확한 원인을 찾기가 힘들어진다 .

**따라서 테스트는 가능하면 작은단위로 쪼개서 집중 해서 할 수 있어한다.**

관심사의 분리라는 원리가 여기에도 적용된다 . 테스트의 관심이 다르다면 테스트 할 대상을 분리하고 집중해서 접근해야한다 .

UserDaoTset는 한가지 과님에 집중할 수있게 작은 단위로 만들어진 테스트다 .

UserDaoTset의 테스트를 수행 할때는 웹인터페이스나 , 그것을 위한 MVC 클래스 , 서비스 오브젝트등이 필요없다 . 에러가 나거나 원치 않는 결과가 나온다면 , 그것은 UserDao 코드나 DB연결 방법 정도에서 문제가 있는것이니 **빠르게 원인을 찾아낼 수 있다.**

**이렇게 작은 단위의 코드에대해 테스트를 수행한 것을 단위테스트 라고한다 .**

일반적으로 단위테스트는 작을수록 좋다 . 단위를 넘어서는 다른 코드들은 신경 쓰지않고 , 참여하지도 않고 테스트가 동작할 수 있다면 좋다.

### 자동수행 테스트 코드

UserDaoTest 의 한 가지 특정은 태스트할 데이터가 코드를 통해 제공되고， 테스트 작업  역시 코드를 통해 자동으로 실행한다는 점이다.

웹 화면에 폼을 띄우고 매번 User 의 등  록 값을 개발자 스스로 입력하고 버튼을 누르고， 또 조회를 위한 ID 값을 넣고 버튼을  누르는 등의 작업을 반복한다면 얼마나 지루하고 불편할까? 간혹 테스트 값 입력에 실  수가 있어서 오류가 나면 다시 테스트를 반복해야 하는 번거로움도 있다.

하지만 UserDaoTest는 자바 클래스의 main() 메소드를 실행하는 가장 간단한 방법  만으로 태스트의 전 과정이 자동으로 진행된다. USER 오브젝트를 만들어 적절한 값을 넣고 , 이미 DB연결 준비 까지 다 되어있는 UserDao오브젝트를 스프링 컨테이너에서 자동으로 가져와서 add()메소드를 호출하고 , 그 키값으로 get()을 호출 하는것까지 자동으로 진행된다.

**자동으로 수행되는 테스트의 장점은 자주 반복 할 수 있다는 것이다 .** 번거로운 작업이 없고 테스트를 빠르게 실행할 수 있기 때문에 언제든 코드를 수정하고 나서 테스트를 해볼수 있다.

### UserDaoTest의 문제점

- 수동작업의 번거로움

  입력한 값과 가져온값이 일치하는지를 테스트 코드가 확인해주지않는다. 단지 콘솔에 값만 출력해 줄 뿐이라 , 결국 그콘솔에 나온값을 보고 확인하는건 사람의 책임이다.

- 실행작업의 번거로움

  아무리 간단하게 실행 가능한 main() 메소드라 하더라도 매번 그것을 실행하는 것은 제법 번거롭다. 만약 DAO가 수백개 가 되고 , 그에 대한 main() 메소드도 그만큼 만들어 진다면 전체 기능을 테스트 해보기 위해 main() 메소드를 수백번 실행하는 수고가 필요하다.

## UserDaoTest 개선

첫번째 문제점인 테스트 결과의 검증 부분을 코드로 만들어보자.

> 수정 전 테스트 코드

```java
System.out.println(user2.getName()); 
System.out.println(user2.getPassword()); 
System.out .println(user2 .getld() + " 조회 성공"); 
```

> 수정 후 테스트 코드 

```java
if (!user.getName().equals(user2 .getName())) { 
	System.out println(" 테스트 실패 (name)‘); 
}
else if (!user.getPassword().equals(user2 .getPassword())) {
	System out.println(" 테스트 실패 (password)") ; 
}
else {
	System.out.println(" 조회 테스트 성공’);
}
```

이렇게 해서 테스트의 수행과 테스트 값 적용 , 그리고 결과를 검증 하는 것까지 모두 자동화 했다.

이 테스트는 UserDao의 두가지 기능이 정상적으로 동작하는지를 언제든지 손쉽게 확인할 수 있게 해준다.

이제 main() 메소드로 만든 테스트는 테스트로서 필요한 기능은 모두 갖춘 셈이다 .

하지만 좀더 편리하게 테스트를 수행하고 결과를 확인하려면 단순한 main() 메소드로는 한계가 있다 .

**일정한 패턴을 가진 테스트를 만들수 있고 , 많은 테스트를 간단히 실행시킬 수 있으며, 테스트 결과를 종합 해서 볼수 있고 , 테스트가 실패한 곳을 빠르게 찾을 수 있는 기능을 갖춘 테스트 지원 도구와 그에 맞는 테스트 작성 방법**이 필요하다.

이미 자바에는 단순하면서도 실용적인 테스트를 위한 도구가 여러가지 존재한다 .

그중에서도 Junit은 이름그대로 자바로 단위 테스트를 만들때 유용하게 쓸 수 있다.

## Junit에 대해서 

지금까지 만들었던 main()  메소드 테스트를 Junit을 이용해 다시 작성해 보겠다.

junit는 프레임 워크다 . 1장에서 프레임 워크의 기본 동작원리가 바로 제어의 역전이라고 설명했다 .

프레임워크는 개발자가 만든 클래스에 대한 제어권한을 넘겨받아서 주도적으로 애플리케이션의 흐름을 제어한다.

개발자가 만든 클래스의 오브젝트를 생성하고 실행하는 일은 프레임워크에 의해 진행된다.

따라서 프레임워크에서 동작하는 코드는 main() 메소드도 필요 없고 오브젝트를 만들어서 실행 시키는 코드도 만들 필요도없다.

> 테스트 메소드 전환
>
> 테스트가 main() 메소드로 만들어졌다는 건 제어권을 직접 갖는다는 의미이기 때문에 main()은 프레임워크에 적용하기 적합하지 않는다. 그래서 가장 먼저 할일은 main() 메소드에 있던 테스트 코드를 일반 메소드로 옮기는 것이다.

```java
public class UserDaoTest {
    
    @Test //Junit에게 테스트용 메소드임을 알려준다 
	public void addAndGet() throws SQLException {	
        //반드시 public로 선언돼야 한다.
		ApplicationContext context = new 
			ClassPathXmlApplicationContext( pplicationContext .x ml ") ; 
		UserDao dao = context.getBean("userDao" , UserDao.class);
		...//생략
	}
}
```

>검증코드전환
>
>aeesrtThat() 메소드는 첫번째 파라미터의 값을 뒤에 나오는 매처라고 불리는 조건으로 일치하면 
>
>다음으로 넘어가고 , 아니면 테스트가 실패하도록 만들어준다.

```java
public class UserDaoTest {
	public void addAndGet() throws SQLException {
		ApplicationContext context = new GenericXmlApplicationContext( 
			appl icationContext .xml"); 
			
		UserD dao = context.getBean("userDao" , UserDao.class); 
		User user = new User(); 
		user. setld ( “gyumee") ; 
		user. setName(" 박성 철 "); 
		user.setPassword("springnol");
		
		dao.add(user); 
		
		User user2 = dao.get(user.getld()); 
		
		assertThat(user2.getName(), is(user.getName())); 
		assertThat(user2.getPassword(), is(user.getPassword()));
	}
}
```

### 테스트 결과의 일관성 

> deleteAll() , getCount()  추가
>
> 매번 User 테이블 데이터를 삭제해줘야한다 . 이전테스트 떄문에 등록된 중복 데이터가 생길수 있음

```java
public void deleteAll() throws SQLException { 
	Connection c = dataSource .gètConnection(); 
	
	PreparedStatement ps = c.prepareStatement(“delete from users ");
	ps.executeUpdate(); 
	ps. close(); 
	c .close();
}
```

```java
public int getCount() throws SQLException {
	Connection c = dataSource.getConnection(); 
	
	PreparedStatement ps = c. prepareStatement("select count(* ) from users");
    
	ResultSet rs = ps .executeQuery(); 
	rs .next(); 
	int count = rs.getlnt(l); 
	
	rs. close(); 
	ps. close(); 
	c .close(); 
	
	return count;
}
```

```java
dao.deleteAll(); 
assertThat(dao.getCount(), is(0));
//User 생성 전에 실행하도록 추가
assertThat(dao.getCount(), is(1)); 
//생성후에 실행하도록 추가
```

동일한 결과를 보장하는 테스트가 만들어 졌다 .하지만 한가지 결과만 검증하고 마는 것은 상당히 위험하다 . getCount( )에 대한 좀더 꼼꼼한 테스트를 만들어 보자 .

> 여러개의 user를 등록해가면서 getCount()의 결과를 매번 확인해보겠다.
>
> 테스트를 만들기 전에 먼저 User 클래스에 한번에 모든 정보를 넣을 수 있도록 초기화가 가능한 생성자를 추가한다 . 

```java
public User(String id, String name , String password) {
		this.id = id; 
		this.name = name; 
		this.password = password;
}
public User(){
	//자바빈의 규약을 따르는 클래스에 생성자를 명시적으로 추가할 경우 
	//파라미터가 없는 디폴트 생성자도 함께 정의 
}
```

```java
@Test
public void count() throws SQLException { 
	ApplicationContext context = new GenericXmlApplicationContext ( 
		"appl icationGontext .xml"); 
		
	UserDao dao = context.getBean("userDao" , UserDao .class); 
	User userl = new User("gyumee" , "박성철“, "springnol "); 
	User user2 = new User("leegw700 ,"길원", "pringno2") ; 
	User user3 = new User("bumjin" ,”박범진" ,"springno3"); 
	
	dao.deleteAll(); 
	assertThat(dao.getCount(), is(0)); 
	
	dao.add(userl); 
	assertThat(dao .getCount() , iS(l)); 
	
	dao .add(user2); 
	assertThat(dao .getCount() , is(2)); 
                          
    dao.add(user3); 
	assertThat(dao.getCount(), is(3));
                          
}
```

> addAndGet() 테스트 보완

```java
@Test
public void addAndGet() throws SQLException {
	UserDao dao = context .getBean ( "userDao" , UserDao .class); 
	userl = new User("gyumee ' , 박성철 'springnol'); 
	User user2 = new User("leegw7ÐÐ" , .이길원， 'springno2'); 
	
	dao .deleteAll(); 
	assertThat(dao .getCount(), is(0)); 
	
	dao.add(user1); 
	dao.add(user2); 
	assertThat(dao.getCount(), iS(2));
	
	User usergetl = dao .get(userl .getld()); 
	assertThat(usergetl.getName(), is(userl.getName())); 
	assertThat(usergetl.getPassword(), is(userl.getPassword()));
	
	User userget2 = dao.get(user2.getld()); 
	assertThat(userget2.getName(), is(user2.getName())); 
	assertThat(userget2.getPassword(), is(user2.getPassword()));
}
```

> get 예외 조건에 대한 테스트 
>
> id값에 해당하는 사용자 정보가 없을 경우 null을 리턴해 주는것과 , 예외를 던지는 방법이 있다 . 여기서는 후자를 써보자.
>
> id 로 get() 을 호출한다. 이때  EmptyResultDataAccessException 이 던져지면 성공이고， 아니라면 실패다. 

```java
@Test(expected=EmptyResultDataAccessException class)
//테스트중에 발생 할것으로 기대하는 예외 클래스 지정
public void getUserFailure() throws SQLException {  
	ApplicationContext context = new GenericXmlApplicationContext ( 
		"applicationContext .xml"); 
                                                  
	UserDao dao = context.getBean("userDao" , UserDao.class); 
	dao.deleteAll(); 
	assertThat(dao.getCount() , is(0)); 
                                                  
	dao.get( "unknown_id"); //예외가 발생해야 성공
}
```

실행 결과는 테스트는 아직 UserDao 코드에는 손을 대지 않았기때문에 실패한다 .

> 테스트를 성공 시키기 위한 코드의 수정

```java
public User get(String id) throws SQLException { 
		
	ResultSet rs = pS .executeQuery(); 
	
	User user = null;
	if (rs.next()) { 
		user = new User(); 
		user.setId(rs.getString('id‘)) ; 
		user.setName(rs.getString("name")); 
		user.setPassword(rs.getString("password")); 
	}
	
	rs. close(); 
	ps .close(); 
	c. c1ose() ;
	
	if (user == null) throw new EmptyResultDataAccessException(l); 
	
	return user;
}
```

### 테스트 코드 개선

> 반복되는 코드 제거 
>
> 애플리케이션 컨텍스트를 만드는 부분과 컨텍스트에서 UserDao를 가져오는 부분 중복제거 .

```java
@Before //메소드가 실행되기 전에 실행돼야하는 메소드 정의  
public void setUp() {
	ApplicationContext context = 
		new GenericXmlApplicationContext("applicationContext.xml"); 
	this.dao = context.getBean("userDao" , UserDao.class);
}
```

1. 테스트 클래스에서 @Test가붙은 public 이고 void형이며 파라미터가 없는 태스트  메소드를모두찾는다. 
2. 테스트 클래스의 오브젝트를 하나 만든다.
3. @Before가 붙은 메소드가 있으면 실행한다. 
4.  @Test 가 붙은 메소드를 하나 호출하고 테스트 결과를 저장해둔다.  
5. . @After가 붙은 메소드가 있으면 실행한다. 
6.  나머지 테스트 메소드에 대해 변을 반복한다.  
7. 모든 테스트의 결과를 종합해서 돌려준다. 

### 픽스처

>테스트를 수행히는 데 펼요한 정보나 오브젝트를 픽스처fixture 라고 한다. 일반적으로 벽  스처는 여러 태스트에서 반복적으로 샤용되기 때문에 @Before 메소드를 이용해 생성해  두면 편리하다.

```java
public class UserDaoTest { 
	private UserDao dao; 
	private User userl; 
	private User user2; 
	private User user3; 
	
	@Before 
	public void setUp() { 
	...//생략
	this.userl = new User(’‘gyumee" ’ "박성철’， .springnol"); 
	this.user2 = new User("leegw7ØØ" , ‘이길원" "springno2"); 
	this.user3 = new User("bumjin" , "박범진 pringno3") ; 
   }
}
```

### 애플리케이션 컨텍스트 관리

> 스프링은 JUnit을 이용하는 테스트 컨텍스트 프레임워크를 제공한다. 태스트 컨텍스트  의 지원을 받으면 간단한 애노테이션 설정만으로 태스트에서 필요로 히는 애플리케이션 컨텍스트를 만들어서 모든 태스트가 공유하게 할 수 있다.

```java
@RunWith(SpringlUnit4ClassRunner.class)
//스프링 테스트의 컨텍스트 프레임워크의 junit 확장기능 지정
@ContextConfiguration(locations="/applicationContext.xm1")
//테스트 컨텍스트가 자동으로 만들어줄 애플리케이션 컨텍스트 위치 지정
public class UserDaoTest {
	@Autowired
	private ApplicationContext context; 
    //스프링 테스트 컨텍스트에 의해 값이 자동으로 주입
	@Oefore 
	public void setUp() {
		this.dao = this.context.getBean( "userDao'’, UserDao.class);
	}
}
```

스프링의 Junit 확장기능은 테스트가 실행되기 전에 딱 한번만 애플리케이션 컨텍스트를 만들어두고, 테스트 오브젝트가 만들어 질때마다 특별한 방법을 이용해 애플리케이션 컨텍스트 자신을 테스트 오브젝트의 특정 필드에 주입해주는것이다.

이렇게하면 처음 애플리케이션 컨텍스트를 생성한뒤 부터는 애플리케이션 컨텍스트를 재사용 할수 있기 때문에 테스트 시간이 매우 짧아진다 .

 여러 개의 테스트 클래스가 있는데  모두 같은 설정파일을 가진 애플리케이션 컨텍스트를 사용한다면， 스프링은 태스트 클래스 사이에서도 애플라케이션 컨텍스트를 공유하게 해준다.

### @Autowired 

@Autowired가 붙은 인스턴스 변수가 있으면 태스트 컨텍스트 프레임워크는 변수 타입과 일치하는 컨텍스트 내의 빈을 찾는다. 타입이 일치하는 빈이 있으면 인스턴스 변수  에 주입해준다.

또 별도의 DI 설정 없이 필드의 타입정보를 이용해 빈을 자동으로 가져올 수 있는데 이런 방법을 타입에 의한 자동와이어령이라고한다.

```java
public class UserDaoTest { 
	@Autowired 
	UserDao dao; //getBean을 사용하는 것이아니라 UserDao빈을 직접 Di받는다.
```

 단， @Autowired는 같은 타입의 빈이 두 개 이상 있는 경우에는  타입만으로는 어떤 빈을 가져올지 결정할 수 없다.

### 테스트 코드에 의한 DI

> 애플리케이션이 사용할 applicationContext. xml 에 정의된 DataSource 빈은 서버  의 DB 풀 서비스와 연결해서 운영용 DB 커넥션을 돌려주도록 만들어져 있다고 해  보자. 
>
>  태스트할 때 이 DataSource를 이용해도 될까? UserDaoTest를 실행하는 순간  deleteAll ()에 의해 운영용 DB의 시용자 정보가 모두 삭제된다면? 결코 일어나서는  안 되는 일이다. 그렇다고 applicationContext.xml 설정을 개발자가 태스트할 때는 테  스트용 DB를 이용하도록 DataSource 빈을 수정했다가， 서버에 배치할 때는 다시 운영  용 DB를 사용하는 DataSource로 수정히는 방법도 있겠지만 번거롭기도 하고 위험할  수도 있다.
>
> **이런 경우엔 테스트 코드에 의한 DI를 이용해서 테스트 중에 DAO가 사용  할 DataSource 오브젝트를 바꿔주는 방법을 이용하면 된다.**
>
> DataSource 구현 클래스는 스프링이 제공하는 가장 빠른 DataSource 인 SingleConnectionDataSource를 사용해보자. SingleConnectionDataSource는 DB  커넥션을 하나만 만들어두고 계속 사용하기 때문에 매우 빠르다. 다중 사용자 환  경에서는 사용할 수 없겠지만 순차적으로 진행되는 태스트에서라면 문제없다.

```java
@DritiesContext //테스트 메소드에서 애플리케이션 컨텍스트의 구성이나 상태를 변경한다는것을
//테스트 컨텍스트 프레임워크에 알려준다.
public class UserDaoTest{
	@Autowired 
	UserDao dao;
	
	@Before 
	public void setUp() (
	...
	DataSource dataSource = new SingleConnectionDataSource( 
	"jdbc:mysql:lllocalhost/testdb" , "spring" ’ "book" , true); 
    //테스트에서 UserDao가 사용할 DateSource오브젝트를 직접 생성
	dao.setDataSource(dataSource); // 코드에 의한 수동 DI 
}
```

이미 애플라케이션 컨텍스트에서  applicationContext.xml 따일의 설정정보를 따라 구성한 오브젝트를 가져와 의존관계  를 강제로 변경했기 때문에  이 방식은 매우 주의해서 시용해야 한다. 

 스프링 테스트 컨텍스트 프레임워크를 적용했다면 애  플리케이션 컨텍스트는 테스트 중에 딱 한 개만 만들어지고 모든 테스트에서 공유해  서 시용한다. 따라서 애플리케이션 컨텍스트의 구성이나 상태를 테스트 내에서 변경하  지 않는 것이 원칙이다.

 그런데 위의 태스트 코드는 애플리케이션 컨돼스트에서 가져온  UserDao 빈의 의존관계를 강제로 변경한다. 한 번 변경하면 나머지 모든 테스트를 수행  하는 동안 변경된 애플리케이션 컨텍스트가 계속 시용될 것이다. 이는 별로 바람직하지  못하다.

그래서 UserDaoTest 에는 @DirtiesContext 라는 애노테이션을 추가해췄다. 이 애노  태이션은 스프링의 태스트 컨텍스트 프레임워크에게 해당 클래스의 태스트에서 애플리  케이션 컨텍스트의 상태를 변경한다는 것을 알려준다.

@DirtiesContext를 이용하면 일단 테스트에서 빈의 의존관계를 강제로 DI 하는 방  법을 시용했을 때 문제는 피할 수 있다. 하지만 이 때문에 애플리케이션 컨텍스트를 매  번 만드는 건 조금 찜찜하다.

**→ xml에 별도의 테스트 전용 파일을 설정을 만들어서 빈으로 등록**

```
<bean id="dataSource" 
	class="org.springframework.jdbc.datasource .SimpleDriverDataSource" > 
	<property name="driverClass" value="com .mysql .jdbc.Driver“ /> 
	<property name="url" value="jdbc:mysql:lllocalhost/testdb" 1> 
	<property name="username" value="spring" /> 
	<property name="password" value="book" /> 
</bean>
```

```java
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration(locations='/test-applicationContext.xml') 
public class UserDaoTest {
```

### 컨테이너 없는 DI 테스트

UserDao나  DataSource 구현 클래스 어디에도 스프링의 API를 직접 사용한다거나 애플리케이션  컨텍스트를 이용히는 코드는 존재하지 않는다. 스프링 01 컨테이너에 의존하지 않는다는 말이다. 따라서 원한다면 스프링 컨테이너를 이용해서 IoC 방식으로 생성되고 DI 되도록 하는 대신， 테스트 코드에서 직접 오브젝트를 만들고  DI 해서 사용해도 된다. 

```
public class UserDaoTest {
	UserDao dao
	
	@Before 
	public void setUp() { 
		dao = new UserDao(); 
		DataSource dataSource = new SingleConnectionDataSource( 
		"jdbc:mysql :lllocalhost/testdb" , pring" ’ "book" , true); 
		dao.setDataSource(dataSource);
	}
```

태스트를 위한 DataSource를 직접 만드는 번거로움은 있지만 애플리케이션 컨텍스트를 아예 시용하지 않으니 코드는 더 단순해지고 이해하기 편해졌다. 애플리케이션 컨  텍스트가 만들어지는 번거로웅이 없어졌으니 그만큼 태스트시간도 절약할 수 있다. 하지만 Junit은 매번 새로운 테스트 오브젝트를 만들기 때문에 매번 새로운 UserDao 오브젝트가 만들어진다는 단점도 있다.

