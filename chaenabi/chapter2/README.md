노션 링크: https://reminiscent-headlight-ee3.notion.site/2-10254862375b481190997faa7a81a17c

# 2장

Created: October 1, 2021 6:00 PM
Tags: 백엔드 스터디

# 2장 테스트



## 2.1 UserDaoTest 다시 보기



### 2.1.1 테스트의 유용성

**테스트는** (리팩토링 과정에서) **변경된 코드가 변경 전의 코드와
완전히 같은 동작을 한다는 것을 확인할 수 있는 유일한 방법이다.**

또한 테스트는 내가 예상하고 의도했던 대로 코드가 정확히 동작하는지를 확인해서,
만든 코드를 확신할 수 있게 해주는 작업이다.

코드에 변경이 있을 때마다 기존 코드와 똑같은 동작을 하는지 확인할 수 없다면
코드를 변경할 때마다 불안할 것이다. 

코드의 결함을 제거해가는 작업을 거치는 동안 테스트를 여러 번 돌려가며
기존 코드와 동일한 작업을 하고 있음을 계속해서 검증받고
최종적으로 테스트가 모두 성공하면 모든 결함이 제거되었다는 확신을 가질 수 있다.

### 2.1.2 UserDaoTest의 특징



아래의 코드는 main() 메소드로 작성된 테스트코드이다.

```java
public class UserDaoTest ( 
	public static void main(String... args) throws SQLException ( 
			ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml"); 
			UserDao dao = context.getBean("userDao", UserDao.class); 

			User user = new User();
			user.setld("user");
			user.setName("백기선"); 
			user.setPassword("married"); 

			dao.add(user);
 
			System.out.println(user.getId() + "등록 성공"); 

			User user2 = dao.get(user.getld()); 
			System.out.println(user2.getName()); 
			System.out.println(user2.getPassword()); 

			System.out.println(user2.getld() + " 조회 성공");
  }
}
```

위 테스트 코드의 내용을 정리해보면 다음과 같다.

- **자바에서 가장 손쉽게 실행 가능한 main() 메소드를 이용한다.**
- **테스트할 대상은 UserDao의 오브젝트를 가져와 메소드를 호출한다.**
- 테스트에 사용할 입력 값(User 객체)를 직접 코드에서 만들어 넣어준다.
- 테스트의 결과를 콘솔에 출력해준다.
- 각 단계의 작업이 에러없이 끝나면 콘솔에 성공 메시지로 출력해준다.

### **웹을 통한 DAO 테스트 방법의 문제점**



브라우저를 켜서 폼을 열고, 값을 입력한 뒤 버튼을 눌러 등록하고, 등록에 문제가 없으면
이번에는 검색 폼이나 다른 URL을 사용해서 등록한 데이터를 조회할 수 있는지 테스트한다.

1. 이런 테스트는 흔히 쓰이는 방법이지만 **시간이 너무 많이 걸린다.** 
기능이 100개이고 하나씩 리팩토링하는 중이라면 몇개만 수정해도 전체가 다 이상없이 
동작하는지 확인하기 위해서 모든 기능을 100번 일일이 테스트해야 할 수도 있다.

1. 가장 큰 문제는 DAO뿐만 아니라 서비스, 컨트롤러, 뷰 등 **모든 레이어의 기능을 다 만들고 나서야 테스트가 가능해진다**는 점이다.

1. 테스트하는 중에 에러가 나거나 테스트가 실패했다면 **어디에서 문제가 발생했는지를 찾아야하는 수고도 필요하다.** 
- 하나의 테스트를 수행하는 데 참여하는 클래스와 코드가 너무 많기 때문이다.
    
    폼을 띄우고 값을 입력하고 등록 버튼을 눌렀는데 에러 메시지가 떴다고 해보자.
    에러 메시지와 호출 스택 정보만 보고 간단히 원인을 찾아낼 수 있을까? 아닌 경우가 더 많다.
    등록이 실패하는 원인은 DB 연결 방법에 문제가 있어서일 수도 있고, DAO 코드가 잘못되어 JDBC API를 잘못 호출해서, 또는 SQL 문법이 틀렸거나, 또는 매개변수 값을 잘못 바인딩했을 수도 있다. DAO의 문제가 아니라 완전히 다른 코드 때문에 테스트가 실패할 수도 있다.
    완전히 성공했지만 결과 메시지를 출력하는 jsp 뷰에 문제가 있어 오류가 난 것처럼 보일 수도 있다. 
    

사실 진짜 테스트하고 싶었던 것은 UserDAO였는데 다른 계층의 코드와 컴포넌트,
심지어 서버의 설정 상태까지 모두 테스트에 영향을 줄 수 있기 때문에
이런 방식으로 테스트하는 것은 번거롭고, 오류가 있을 때 빠르고 정확하게 대응하기가 힘들다.

그러면 테스트를 어떻게 만들어야 상기한 문제들을 피할 수 있고 효율적으로 테스트를 활용할 수 있을까?

### 작은 단위의 테스트 (단위테스트—Unit Test)



테스트하고자 하는 대상이 명확하다면 그 대상에만 집중해서 테스트하는 것이 바람직하다.
한꺼번에 너무 많은 것을 몰아서 테스트하면 테스트 수행 과정도 복잡해지고 오류가 발생했을 때 정확한 원인을 찾기가 힘들어진다.

따라서 테스트는 가능하면 작은 단위로 쪼개서 집중해서 할 수 있어야 한다.
관심사의 분리라는 원리가 여기에도 적용된다.

테스트의 관심이 다르다면 테스트할 대상을 분리하고 집중해서 접근해야 한다.

위에서 살펴본 UserDaoTest는 한 가지 관심에 집중된 작은 단위의 테스트다.
UserDaoTest의 테스트를 수행할 때는 웹 인터페이스나, 그것을 위한 MVC 클래스, 서비스 객체등이 필요없다. 서버에 배포할 필요도 없다. 이렇게 작은 단위의 코드에 대해 테스트를 수행한 것을 

**단위테스트**라고 부른다.

여기서 말하는 단위란 얼마나 큰지, 범위가 어느 정도인지는 딱 정해지지 않는다.

사용자 관리 기능을 모두 통틀어 하나의 단위로 볼 수도 있고, 또는 UserDao의 add() 메소드 하나만을 하나의 단위라고 생각할 수도 있다. 

어쩄든 충분히 하나의 관심에 집중해서 효율적으로 테스트할 만한 범위의 단위라고 보면 된다.

일반적으로 단위는 작을수록 좋다.

### 자동수행 테스트 코드



UserDaoTest의 한가지 특징은 테스트할 데이터가 코드를 통해 제공되고 테스트 작업 역시 자동으로 실행한다는 점이다. 테스트를 위해 개발자가 할 일은 main() 메소드를 실행하기만 하면 될 뿐이다.
자동으로 수행되는 테스트의 장점은 자주 반복할 수 있다는 것이다. 시간도 오래걸리지 않는다.

반면 웹 화면에 폼을 띄우고 매번 User의 등록 값을 개발자 스스로 입력하고, 버튼을 누르고,
조회를 위한 ID 값을 넣고, 버튼을 누르는 작업을 코드 변경시마다 반복해야한다면
얼마나 지루하고 불편할까? 중간에 실수라도 하면 다시 테스트를 반복해야할지도 모른다.

한 4~5번만 이런 작업을 반복하고 나면, 귀찮아서라도 더 이상 코드를 수정하거나 개선하려는 마음을 접어버리고 그냥 잘 돌아가겠지 하고 넘어가고 싶을지도 모르겠다.

이렇게 테스트는 자동으로 수행되도록 코드로 만들어지는 것이 중요하다.
단, 애플리케이션을 구성하는 클래스 안에 테스트 코드를 (이를테면 main() 메소드 등) 포함시키기 보다는 별도의 테스트용 클래스를 만들고 그 안에 테스트 코드를 넣는 편이 낫다.

### 지속적인 개선과 점진적인 개발을 위한 테스트



처음 만든 초난감 DAO 코드를, 스프링을 이용한 깔끔하고 완성도 높은 객체지향적 코드로 발전시키는 과정의 일등 공신은 바로 이 테스트였다. 테스트가 없었다면 다양한 방법을 동원해서 코드를 수정하고 설계를 개선해나가는 과정이 힘들었을 것이다. 
DAO 코드를 만들자마자 바로 DAO로서의 기능에 문제가 없는지 검증해주는 테스트 코드를 만들어뒀기 때문에, 코드를 개선할 때마다 테스트코드를 돌려서 잘 되는지 확인받을 수 있었고, 그 덕분에 자신감을 가지고 계속 코드를 수정할 수 있었다.

만약 처음부터 스프링을 적용하고 XML로 설정을 만들고 모든 코드를 다 만들고 난 뒤에 이를 검증하려고 했다면, 아마 쏟아지는 에러 메시지에 기가 질려서 무엇을 해야할지 몰라 막막해졌을지도 모르겠다. 스프링이 뿌려대는 희한한 에러 메시지를 보고나서, 이게 스프링을 잘못 사용한 것인지, 설정에 오류가 있는 것인지, DAO 코드가 잘못된건지 찾아내는데 적지 않은 고민스러운 시간을 보내야 했을 것이다. 

### 2.1.3 UserDAOTest의 문제점



UserDAOTest가 UI까지 동원되는 수동 테스트에 비해 장점이 많은 건 사실이지만,
만족스럽지 못한 부분도 있다.

- **수동 확인 작업의 번거로움**
add()에서 User의 정보를 DB에 저장하고, 다시 get()을 이용해 가져왔을때 입력한 값과 가져온 값이 일치하는지를 테스트 코드는 알려주지 않는다. 콘솔에 값만 출력해줄 뿐이다.
결국 그 값을 보고 등록과 조회가 성공적인지 확인하는 것은 사람의 책임이다.
다시 말해 테스트 수행은 자동이지만 결과를 확인하는 것은 사람의 책임이기 때문에 완전한 자동 테스트라고는 말할 수 없다.
단지 몇가지 필드의 값이라면 사람이 해도 별 수고가 아닐수 있지만 검증해야 하는 양이 많고 복잡하다면 불편함을 느낄 수 밖에 없다. 작은 차이를 발견하지 못하고 넘어가는 실수가 발생할 수 도 있다.
- **실행 작업의 번거로움**
아무리 간단히 실행 가능한 main() 메소드라고 해도 그것을 매번 실행하는 것은 제법 힘들다.
DAO가 수백 개가 되고 그에 대한 main 메소드도 그만큼 만들어진다면 main() 메서드를 수백번 실행해야하는 수고가 필요해진다. 또한 그결과를 눈으로 확인해서 기록하고, 테스트 결과를 정리하려면 이것도 제법 큰 작업이 된다.

## 2.2 UserDaoTest 개선



```java
//from

System.out.println(user2.getName()); 
System.out.println(user2.getPassword()); 
System.out.println(user2.getld() + " 조회 성공");
```

1차 개선

 

```java
if (!user.getName().equals(user2.getName())) { 
		System.out println(" 테스트 실패 (name) "); 
else if (!user.getPassword().equals(user2.getPassword())) ( 
		System out.println(" 테스트 실패 (password)"); 
else { 
	System.out.println("조회 테스트 성공");
}
```

if문이 너무 많다. 다시 한번 더 개선 (JUnit 테스트로 전환)

```java
Assertions.assertEquals(user.getName(), user2.getName());
Assertions.assertThat(user.getPassword()).isEqualTo(user2.getPassword());
```

```java
// Junit에서 테스트가 실패했을 경우 아래와 같은 메시지로 결과를 출력해준다.
Time: 1.094 
There was 1 failure: 
1) testMethod1(springbook.dao.UserDaoTest) 
java.lang.AssertionError: 
Expected: is "박성철" 
		 got: null 
160 at springbook.dao.UserDaoTest.main(UserDaoTest.java:36) 
FAILURES!!!
Tests run: 1. Failures: 1
```

```java
// 용례 2

@Test 
public void count() throws SQLException { 
		ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml"); 
		UserDao dao = context.getBean("userDao", UserDao.class);
		User userl = new User("gyumee" , "박성철"， "springnol"); 
		User user2 = new User("leegw700", "이길원", "springno2"); 
		User user3 = new User("bumjin", "박범진", "springno3"); 
		
		dao.deleteAll(); 
		assertThat(dao.getCount(), is(0));

		dao.add(userl); 

		assertThat(dao.getCount(), is(1)); 

		dao.add(user2); 
		assertThat(dao.getCount(), is(2)); 

		dao.add(user3);
		assertThat(dao.getCount(), is(3)); 
}

//또는..

public class UserDaoTest {

	@Autowired
	UserDao dao; // 빈 주입 사용

	@Test 
	public void count() throws SQLException { 
			User userl = new User("gyumee" , "박성철"， "springnol"); 
			User user2 = new User("leegw700", "이길원", "springno2"); 
			User user3 = new User("bumjin", "박범진", "springno3"); 
			
			dao.deleteAll(); 
			assertThat(dao.getCount(), is(0));
	
			dao.add(userl); 
	
			assertThat(dao.getCount(), is(1)); 
	
			dao.add(user2); 
			assertThat(dao.getCount(), is(2)); 
	
			dao.add(user3);
			assertThat(dao.getCount(), is(3)); 
	}
}
```

### BeforeEach, AfterEach



```java
@BeforeEach
public void setUp() { 
	System.out.println("각각의 테스트 메서드가 실행되기 전에 한번'씩' 실행한다.");
}

@AfterEach
public void after() {
	System.out.println("각각의 테스트 메서드가 실행되고난 후 한번씩 실행된다.");
}
	
```

## 2.5 학습 테스트로 배우는 스프링



### JUnit 테스트: 객체 테스트



- JUnit은 테스트 메소드를 수행할 때마다 새로운 객체를 만든다. 정말로 그럴까?
혹시 업데이트되어서 달라지지는 않았을까? 직접 테스트해보자.

```java
public class JUnitTest ( 
		static JUnitTest testObject; 

		@Test
		public void testl() { 
				assertThat(this, is(not(samelnstance(testObject)))); 
				testObject = this; 
		}
		
		@Test
		public void test2() ( 
				assertThat(this, is(not(samelnstance(testObject)))); 
				testObject = this; 
		}

		@Test
		public void test3() ( 
				assertThat(this, is(not(samelnstance(testObject)))); 
				testObject = this;
		}

   // 세가지 테스트 모두 성공.
}
```

### 스프링 테스트: 컨텍스트 테스트



스프링의 테스트용 애플리케이션 컨텍스트는 테스트 개수에 상관없이 한개만 만들어지며,
또 이렇게 만들어진 컨텍스트는 모든 테스트에서 공유된다. 정말로 그럴까? 테스트해보자.

```java
<?xml version="1.0" encoding="UTF-8"?> 
<beans mlns="http://www.springframework.org/schema/beans"
			xmlns:xsi="http://www .w3.org/2001/XMLSchema-instance" 
			xsi:schemaLocation="http://www.springframework.org/schema/beans 
												 http://www.springframework.org/schema/beans/spring-beans.xsd"> 
</beans>
```

```java
@Extension(SpringBootExtension.class) 
@ContextConfiguration(location="/test-application.xml")
public class JUnitTest { 
		
		@Autowired
		ApplicationContext context; // 진짜로 하나만 만들어지는지 테스트로 확인해보자.

		static Set<JUnitTest> testObjects = new HashSet<JUnitTest>(); 
		static ApplicationContext contextObject = null; 
		
		@Test
		public void testl() {
			assertThat(testObjects, not(hasltem(this))); 
			testObjects.add(this); 

			assertThat(contextObject == null || contextObject == this.context, is(true)); 
			contextObject = this.context; 
		}

		@Test
		public void test2() {
			assertThat(testObjects, not(hasltem(this))); 
			testObjects.add(this); 
	
			assertTrue(contextObject == null || contextObject == this.context); 
			contextObject = this.context; 
		}

		@Test
		public void test3() ( 
			assertThat(testObjects, not(hasltem(this))); 
			testObjects.add(this); 
			assertThat(contextObject, either(is(nullValue())).or(is(this.context))); 
			contextObject = this.context; 
		}

		// 모두 성공한다.
}
```

## 2.6 정리

이 장에서는 다음과 같이 테스트의 펼요성과 작성 방법을 살펴봤다.

- 태스트는 자동화되어야 하고, 빠르게 실행할 수 있어야 한다 .
- main() 테스트 대신 JUnit 프레임워크를 이용한 태스트 작성이 편리하다.
- 태스트 결과는 일관성이 있어야 한다. 코드의 변경 없이 환경이나 태스트 실행 순서에 따라
서 결과가달라지면안된다.
- 테스트는 포괄적으로 작성해야 한다. 충분한 검증을 하지 않는 테스트는 없는 것보다 나뿔
수있다.
- 코드 작성과 테스트 수행의 간격이 짧을수록 효과적이다.
- 테스트하기 쉬운 코드가 좋은 코드다.
- 태스트를 먼저 만들고 태스트를 성공시키는 코드를 만들어가는 태스트 주도 개발 방법도 유
용하다.
- 테스트 코드도 애플리케이션 코드와 마찬가지로 적절한 리팩토령이 필요하다 .
- @BeforeEach, @AfterEach를 사용해서 테스트 메소드들의 공통 준비 작업과 정리 작업을 처리할 수 있다.
- 스프링 태스트 컨텍스트 프레임워크를 이용하면 태스트 성능을 향상시킬 수 있다.
- 동일한 설정파일을 사용하는 테스트는 하나의 애플리케이션 컨텍스트를 공유한다 .
- @Autowired를 사용하면 컨텍스트의 빈을 태스트 오브젝트에 DI 할 수 있다.
- 기술의 사용 방법을 익히고 이혜를 돕기 위해 학습 태스트를 작성하자.
- 오류가 발견될 경우 그에 대한 버그 태스트를 만들어두면 유용하다.

스프링을 사용하는 개발자라면 자신이 만든 코드를 태스트로 검증하는 방법을 알고
있어야 하, 테스트를 개발에 적극적으로 활용할 수 있어야 한다.