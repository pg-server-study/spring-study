[노션을 참고하면 더욱 좋습니다.](https://near-sunscreen-c35.notion.site/2-b28a6b1bf46b4523959dc86cb5aa111e)

# 2장 테스트


## 2.1 UserDaoTest 다시 보기

### 2.1.1 테스트의 유용성

1장 에서 만든 UserDao가 기대했떤 대로 동작하는지 확인하기 위해 간단한 테스트 코드를 만들었다.

만든 코드는 어떤 방식으로든 테스트 해야한다.

이전에 만든 코드는 main() 메소드를 이용하여 add(), get() 메소드를 호출하여 눈으로 테스트를 진행하였다.

테스트코드를 실행해가면서 초난감 UserDao코드의 설계와 코드를 리팩토링하였고

리팩토링 하면서 코드를 개선해도 이전과 다를거 없이 기능이 동작하는거를 확인 할 수 있었다.

프로그래머가 테스트코드를 실행하지 않고, 머릿속으로 시뮬레이션 하는 보장되지 않은 방법으로

코드를 작성하는것과는 차원이 다른 안정성을 보장해준다.

테스트코드를 작성함으로 프로그래머가 의도한대로의 기능이 완성되었는지

다른사람이 테스트코드를 실행함으로써 코드의 동작원리를 확인하는것처럼

테스트 코드의 유용점은 어마어마하다.



### 2.1.2 UserDaoTest의 특징

1장에서 만들었떤 main(0 메소드로 작성된 테스트 코드다.

이 테스트 코드를 살펴보자

```
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

위의 테스트 코드를 정리해보면 다음과 같다.

- 자바에서 가장 손쉽게 실행 가능한 main() 메소드를 이용한다.
- 테스트할 대상인 UserDao의 오브젝트를 가져와 메소드를 호출한다.
- 테스트에 사용할 입력 값(User 오브젝트) 을 직접 코드에서 만들어  넣어준다.
- 테스트의 결과를 콘솔에 출력해준다.
- 각 단계의 작업이 에러 없이 끝나면 콘솔에 성공 메시지로 출력해준다.

이 테스트 방법에서 가장 돋보이는건, main(0 메소드를 이용해 쉽게 테스트 수행을 가능하게 했다는 점과 테스트할 대상인 userDao를 직접 호출해서 사용한다는 점이다.

**웹을 통한 DAO 테스트 방법의 문제점**

보통 웹 프로그램에서 사용하는 DAO를 테스트하는 방법은 다음과 같다.

DAO를 만든 뒤 바로 테스트하지않고 서비스, MVC 프레젠테이션 계층까지 포함된 모든

입출력 기능을 다 만든 후 웹 화면을 띄우고 form 에 값을 입력하고 폼의 입력값에 대한 객체를 만들고 UserDao를 호출하여 테스트를 진행한다.

이런 방식으로 테스트를 하여 에러가 발생하면 에러 메시지와 호출 스택정보만으로 원일을 찾기가 힘들다. DB 연결 에러인지 SQL 문법이 틀린지 이런방식의 테스트는 정말 불편하고 오류가 있을 때 빠르게 대응 하기가 힘들어진다.

웹을 통한 DAO 방식의 문제점 정리

- 하나의 테스트를 위해 모든 계층을 다 만들어줘야 한다.
- 무언가를 테스트 하려면 웹화면을 하나 간단하게 만들어야한다.
- 에러가 발생할경우 추적이 힘들고 대응도 힘들어진다.

이러한 테스트 방식말고 보다 효율적으로 테스트를 활용 하는 방법을 알아보자.

**작은 단위의 테스트**

테스트 하고자 하는 대상이 명확하다면 그 대상에만 집중해서 테스트 하는것이 바람직하다.

한꺼번에 너무 많은 것을 몰아서 테스트하면 테스트 준비도 길어지고 수행과정도 복잡하고

오류를 찾기도 힘들어진다.



따라서 테스트는 가능하면 작은 단위로 쪼개서 집중해야한다.

UserDaoTest는 한 가지 관심에 집중할 수 있게 작은 단위로 만들어진 테스트다

UserDaoTest를 수행 할때 웹 인터페이스나, 그것을 위한 MVC 클래스 서비스 오브젝트등이 필요없다.

UserDaoTest에서 에러가난다면 UserDao 코드나 DB 연결 방법등 에러의 추적이 훨씬 좁아지며

쉬워진다.

이 처럼 작은 단위의 코드에 대해 테스트를 수행하는것을 단위 테스트라고 한다.

여기서 단위란 무엇인지 그 크기와 범위가 어느정도인지 정해진건 아니다.

UserDao의 add() 메소드 하나에대한 테스트도 단위테스트가 될 수 있다.

이렇게 단위로 나눠서 테스트하는 단위 테스트가 필요한 이유를 좀 더 생각해보자.

```
다른 책을 보다 본 문구가 있다.

테스트 코드를 작성한 개발자와 작성하지 않은 개발자가 있다.

누군가 질문했다 XXX기능에서 X값을 넣고 실행하면 어떤 결과가 나오나요?

테스트 코드를 작성한 개발자는 자신이 만든 테스트코드에 x값을 넣고 바로 실행해보면된다.

하지만 테스트 코드를 작성하지 않은 개발자는 위의 내용처럼 서버를 키고..
웹 인터페이스에 들어가서 form 안에다 x값을 넣고 실행을 해야한다.

이 얼마나 많은 시간이 소요되는것인가? 

또한 테스트 코드를 작성함으로 다른 개발자가 해당 기능이 어떤 기능인지도 보기 편해진다.

즉 유지보수가 쉬워지고 코드의 안정성도 올라간다.
```

이렇게 테스트 코드는 매우 중요하다.

**자동수행 테스트 코드**

UserDaoTest의 한 가지 특징은 테스트할 데이터가 코드를 통해 제공되고 테스트 작업 역시 코드를 통해 자동으로 실행한다는 점이다.

웹 하면에 폼을 띄우고 매번 User의 등록 값을 개발자가 스스로 입력하고 버튼을 누르고,

또 조회를 위한 ID 값을 넣고 버튼을 누르는 등의 작업을 반복하면 얼마나 불편하고 지루할까?

하지만 main() 메소드는 테스트를하는데 1초미만밖에 걸리지 않는다

하루에 100번을 돌려도 2분도 안걸린다. 그러니 테스트를 자주 수행해도 부담이 없고

특정 기능이 잘 동작하는지 확인하는데도 금방 할 수 있다.

어떤 개발자는 모든 클래스는 스스로 자신을 테스트하는 main() 메소드를 갖고 있어야 한다고

주장한다.

굳이 모든 클래스의 main() 메소드가 필요할까?

현재는 UserDaoTest 밖에 존재하지 않지만 테스트가 많아지면 그것을 관리하는거 또한 힘들다

그러므로 테스트 코드를 넣을 별개의 클래스로 분할하여 관리를 해야 한다.

**지속적인 개선과 점진적인 개발을 위한 테스트**

처음 만든 초난감 DAO 코드를, 스프링을 이용한 깔끔하고 완성도 높은

객체지향적 코드로 발전시키는 과정의 일등공신은 바로 이 테스트였다.

테스트가 없었더라면 리팩토링을 하면서 리팩토링한 코드가 기존 실행과 동일하게

돌아가는지 확인할 수 없었고 또 확인하려면 많은 과정을 거쳐야 함으로

리팩토링 시간도 늘어날 것이다.

만약 처음부터 스프링을 적용하고 XML로 설정을 만들고 모든 코드를 다 만들고

난 뒤에 이를 검증하였다면 쏟아지는 에러 메시지에 기가 질려서 무엇을 해야 할지

몰라 막막해졌을지도 모른다.

하지만 단순 무식하게 코드를 만들고 테스트를 만들어두고 작은 단계를 거치면서

테스트코드로 확인을 하고 개선하였기에 작업속도가 빨라지고 더욱 쉽게 리팩토링을 할수 있었던 것이다.



### 2.1.3 UserDaoTest의 문제점

앞에서 살펴봤듯이 UserDaoTest가 UI까지 동원되는 번거로운 수동 테스트에 비해

장점이 많은건 사실이지만 만족스럽지 못한 부분도 있다

**수동 확인 작업의 번거로움**

- 여전히 사람의 눈으로 확인해야한다.
- DB에서 가져온 값이 일치하는지를 테스트 코드가 확인해주지 않는다.
- 개발자가 테스트코드를 확인하고 책임져야 한다.

**실행 작업의 번거로움**

- 매번 main을 실행해야한다.
- 테스트가 무수히 많아지면 그 많은걸 다 실행해야한다.



## 2.2 UserDaoTest 개선

### 2.2.1 테스트 검증의 자동화

테스트 코드를 개선해보자

개선 전

```
System.out.println(user2.getName());
System.out.println(user2.getPassword());
System.out.println(user2 .getld() + " 조회 성공 ")
```

개선 후

```
if (!user.getName().equals(user2.getName())) {
		System.out.println("테스트 실패 (name)");
} else if (!user.getPassword().equals(user2.getPassword())) {
		System.out.println("테스트 실패 (password)");
} else {
		System.out.println("조회 테스트 성공");
}
```

이제 좀더 테스트가 자동화 되었다.

이제 개발자가 할일은 값을 확인 하는 것이 아닌 **테스트 성공 이라고** 나오는지만 확인 하면 된다.

하지만 테스트 성공 까지 개발자가 눈으로 확인 해야 한다.

이를 좀더 편하게 성공과 실패로 나누어주는 프레임워크가 있다

JUnit 를 알아보자

### 2.2.2 테스트의 효율적인 수행과 결과 관리

JUnit을 사용하여 테스트 코드를 작성해보겠다

JUnit 프레임워크가 요구하는 조건 두가지를 따라야한다

첫째는 메소드가 public으로 선언돼야 하는 것이고

다른 하나는 메소드에 @Test 라는 애노테이션을 붙여 줘야 한다.

그리고 값의 검증을 도와주는 assertThat 이라는 스태틱 메소드를 이용 할 수 있다

assertThat 는 아래와 같이 사용할수 있다.

```
assertThat(user1.getName()).isEqual(user2.getName());
```

이 두가지를 활용하여 기존에 있던 코드를 바꿔보겠따

```
여기서 책에는 스프링으로 되어있지만 저는 스프링 부트와 JUnit5를 이용하여
진행 하도록 하겠습니다.
```

```java
package com.example.springtobi.chapter1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserDAOTest {

    @Autowired
    UserDao userDao;

    @Test
    @DisplayName("데이터베이스에 유저 등록 및 조회 테스트")
    void addUserTest() throws ClassNotFoundException {

        // given
        User user = new User();
        user.setId("whiteship");
        user.setName("백기선");
        user.setPassword("married");

        // when
        userDao.add(user); // 유저 등록 실행
        User savedUser = userDao.get(user.getId()); // 유저 조회 실행

        // then (저장하고자 했던 유저의 이름과 실제로 DB에 저장된 유저의 이름이 같은지 검증하라)
        assertThat(user.getName()).isEqualTo(savedUser.getName());

    }

}
```

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter2/image/Untitled.png?raw=true)

성공한다면 위와 같이 성공표시가 뜨게된다.

아래는 assertThat 와 각종 JUnit 애노테이션을 정리해 두겠습니다.

```java
package com.example.springtobi.chapter2;

import org.junit.jupiter.api.*;

public class JUnitTest {

    @BeforeAll
    static void beforeAll() {
        System.out.println("테스트가 실행되기 전 단 한번만 실행 됩니다.");
        System.out.println();
    }

    @BeforeEach
    public void beforeEach() {
        System.out.println("테스트 코드별로 실행되기 전에 실행 됩니다.");
    }

    @Test
    public void firstTest() {
        System.out.println("첫번째 테스트");
    }

    @Test
    public void secondTest() {
        System.out.println("두번째 테스트");
    }

    @AfterEach
    public void afterEach() {
        System.out.println("테스트 코드별로 실행된 후 실행 됩니다.");
        System.out.println();
    }

    @AfterAll
    static void afterAll() {
        System.out.println("테스트가 종료되기 전 단 한번만 실행 됩니다.");
    }

}
```

위 코드의 실행 로그입니다.

```
테스트가 실행되기 전 단 한번만 실행 됩니다.

테스트 코드별로 실행되기 전에 실행 됩니다.
첫번째 테스트
테스트 코드별로 실행된 후 실행 됩니다.

테스트 코드별로 실행되기 전에 실행 됩니다.
두번째 테스트
테스트 코드별로 실행된 후 실행 됩니다.

테스트가 종료되기 전 단 한번만 실행 됩니다.
```

| 메소드 | 설명 |
| -- | - |
| isEqualTo(obj) | obj와 같다 |
| isNotEqualTo(obj) | obj와 다르다 |
| isEqualToIgnoringCase(str) | 파라미터와 같다  ( 대소문자 무시) |
| contains(str) | 파라미터를 포함한다. |
| containsIgnoringCase(str) | 파라미터를 포함한다( 대소문자 무시) |
| doesNotContain(str) | 파라미터를 포함하지 않는다. |
| startsWith(str) | 파라미터로 시작한다 |
| doesNotStartWith(str) | 파라미터로 시작하지 않는다. |
| endsWith(str) | 파라미터로 끝난다 |
| doesNotEndWith(str) | 파라미터로 끝나지 않는다. |
| matches(regex) | regex 정규식과 같다 |
| doesNotMatch(regex) | regex 정규식과 같지 않다 |
| isLowerCase(str) | 파라미터는 소문자로 이루어져 있다 |
| isUpperCase(str) | 파라미터는 대문자로 이루어져 있다. |
| isZero(n) | 파라미터는 0이다 |
| isNotZero(n) | 파라미터는 0이 아니다 |
| isOne(n) | 파라미터는 1이다 |
| isPositive(n) | 파라미터는 양수 이다 |
| isNegative(n) | 파라미터는 음수 이다 |
| isBetween(start, end) | start와 end 사이의 값 이다 |
| isStrictlyBetween(start,end) | start와 end 사이의 값이 아니다 |
| isCloseTo(n within or offset) | 주어진 within 또는 offset 에 가까운 값이다. |
| isNotCloseTo(n, byLessThan or offset) | 주어진 within 또는 offset 에 가까운 값이 아니다 |
| isCloseTo(n, withinPercentage) | 주어진 백분율 내에서 주어진 숫자에 가깝다 |
| isNotCloseTo(n, withinPercentage) | 주어진 백분율 내에서 주어진 숫자에 가깝지 않다 |
| isTrue() | 참이다 |
| isFalse() | 거짓이다 |
| isNull() | null 값 이다 |
| isNotNull() | null 이 아니다 |
| isBlank() | 빈 값이다 |
| isNotBlank() | 빈 값 이 아니다 |
| isEmpty() | 빈 값이다 ( 공백 포함 ) |
| isNotEmpty() | 빈 값이 아니다 ( 공백 미 포함 ) |
| isNotOrEmpty() | 빈 값이 아니다 (공백 미포함 ) |
| isNullOrEmpty() | null 값  이거나 빈 값 이다 (공백 포함 ) |
| isLessThan(str) | str보다 낮은 문자열 이다 (아스키 코드) |
| isln(..obj) | 여러개의 obj 중 1개와 같다 |
| isNotln(..obj) | 여러 obj 와 모두 다르다 |
| filteredOn(..) | list필터 |
| extracting(..) | list 프로퍼티 값 |


## 2.5 학습 테스트로 배우는 스프링

개발자가 자신이 만든 코드가 아닌 다른 사람이 만든 코드와 기능에 대한 테스트를 작성할 필요가 있을까?

일반적으로 애플리케이션 개발자는 자신이 만들고 있는 코드에 대한 테스트만 작성하면 된다.

하지만 때로는 자신이 만들지 않은 프레임워크나 다른 개발팀에서 제공한 라이브러리등에

대해서도 테스트를 작성해야 한다. 이런 테스트를 학습 테스트 라고 한다.

### 2.5.1 학습 테스트의 장점

**다양한 조건에 따른 기능을 손쉽게 확인해 볼 수 있다.**

- 자동화된 테스트의 모든장점이 학습 테스트에도 적용된다.
- 다양한 조건에서 어떻게 동작하는지 빠르게 확인 할 수 있다.

**학습 테스트 코드를 개발 중에 참고할 수 있다**

- 학습 테스트를 하며 남겨둔 코드를 실제 개발에서 샘플 코드로 참고할수 있다.
- 기록이 남는다. 즉각 이전에 테스트 해봤던 예제를 보며 참고 할 수 있다.

**프레임 워크나 제품을 업그레이드 할 때 호환성 검증을 도와준다.**

- 새로운 버전으로 업그레이드 할때 테스트 코드가 이를 체크해준다.
- 새로운 라이브러리나 프레임워크를 도입할 때 기존에 코드에 영향을 미치는걸 확인 할 수 있다.

```
이건 개인적인 경험입니다 
사이드 프로젝트를 진행하다 자바8 -> 11로 버전업을 하다보니
QueryDSL 에서 문제가 생겼습니다.
QueryDSL의 테스트 코드를 작성해두어 빌드를 돌릴떄 테스트 코드에서
QueryDSL 이 돌아가지 않는걸 확인 할 수 있었습니다.

만약 테스트 코드를 작성하지 않았더라면 확인 못한채로 배포가 되었겠지요.
```

**테스트 작성에 대한 좋은 훈련이 된다**

- 개발자가 테스트를 작성하는데 미흡하다면 좋은 경험이 될 수 있다.
- 더 좋은 테스트 코드를 작성할 수 있게 된다.

**새로운 기술을 공부하는 과정이 즐거워진다.**

- 책이나 문서로만 보는게 아닌 직접 테스트 코드를 만들며 실습 할 수있다.
- 개발에 대한 흥미가 올라간다

### 2.5.2 학습 테스트 예제

**JUnit 테스트 오브젝트 테스트**

JUnit은 테스트 메소드를 수행할 때마다 새로운 오브젝트르 만든다고 했다.

그런데 정말 매번 새로운 오브젝트가 만들어질까?

궁금하다면 JUnit에 대한 학습 테스트를 만들어보자

```java
package com.example.springtoby.chapter2;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

public class JUnitTest {

    static JUnitTest testObject;

    @Test
    public void test1() {
        assertThat(this).isNotSameAs(testObject);
        testObject = this;
    }

    @Test
    public void test2() {
        assertThat(this).isNotSameAs(testObject);
        testObject = this;
    }

    @Test
    public void test3() {
        assertThat(this).isNotSameAs(testObject);
        testObject = this;
    }

}
```

위의 코드는 전부 성공한다.

**스프링 테스트 컨텍스트 테스트**

스프링 테스트 컨텍스트 프레임워크에 대한 학습 테스트를 만들어 보자.

스프링의 테스트용 애플리케이션 컨텍스트는 개수의 상관없이 한개만 만들어준다

테스트 코드를 만들어서 테스트 해보자!

```java
package com.example.springtoby.chapter2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration
public class SpringContextTest {

    @Autowired
    ApplicationContext context;

    static ApplicationContext contextObject = null;

    @Test
    public void test1() {
        assertThat(contextObject == null || contextObject == this.context).isTrue();
        contextObject = this.context;
    }

    @Test
    public void test2() {
        assertThat(contextObject == null || contextObject == this.context).isTrue();
        contextObject = this.context;
    }

    @Test
    public void test3() {
        assertThat(contextObject == null || contextObject == this.context).isTrue();
        contextObject = this.context;
    }

}
```

JUnit5로 만든 테스트 코드다 전부 통과한다.

### 2.5.3 버그 테스트

버그테스트란 코드에 오류가 잇을 때 그 오류를 가장 잘 드러내줄 수 있는 테스트를 말한다.

QA팀의 테스트 중에 기능 오류가 발견 됐다고 하자, 또는 사용자가 버그가 있다고

알려온 경우도 좋다. 이떄 코드를 뒤져가며 수정하지말고 버그테스트를 만들자

버그 테스트는 일단 실패하도록 만들어야 한다.

버그가 원인이 되서 테스트가 실패하는 코드를 만드는 것이다.

그리고 나서 버그 테스트가 성공하도록 애플리케이션 코드를 수정한다.

버그 테스트의 장점

**테스트의 완성도를 높여준다.**

- 기존 테스트에서 미처 검증하지 못했던 부분을 보완해준다
- 이후에 비슷한 문제가 등장해도 쉽게 추적이 가능하다.

**버그의 내용을 명확하게 분석하게 해준다**

- 어떤 이유로 버그가 생겼는지 효과적으로 분석할 수 있다.
- 버그로 인행 발생할 수 있는 다른 오류를 함께 발견할 수 있다.

**기술적인 문제를 해결하는데 도움이 된다.**

- 코드와 설정등을 살펴봐도 문제가 없는거 같이 느껴지거나 또는 기술적으로 다루기 힘든 버그를 발견 하는 경우도 있다.

## 2.6 정리

- 테스트는 자동화돼야 하고, 빠르게 실행할 수 있어야 한다.
- main() 테스트 대신 JUinit 프레임워크를 이용한 테스트 작성이 편리하다
- 테스트 결과는 일관성이 있어야 한다. 코드의 변경 없이 환경이나 테스트 실행 순서에 따라 결과가 달라지면 안된다.
- 테스트는 포괄적으로 작성해야 한다. 충분한 검증을 하지 않는 테스트는 없는 것보다 나쁠 수 있다.
- 코드 작성과 테스트 수행의 간격이 짧을수록 효과적이다.
- 테스트하기 쉬운 코드가 좋은 코드다
- 테스트를 먼저 만들고 테스트를 성공시키는 코드를 만들어가는 테스트 주도 개발 방법도 유용하다
- 테스트코드도 애플리케이션 코드와 마찬가지로 적절한 리팩토링이 필요하다
- @Before @After를 사용해서 테스트 메소들의 공통 준비 작업과 정리 작업을 처리할 수 있다.
- 스프링 테스트 컨텍스트 프레임워크를 이용하며 테스트 성능을 향상시킬 수 있다.
- @AutoWired를 사용하면 컨텍스트 빈을 테스트 오브젝트에 DI 할 수 있다.
- 기술의 사용 방법을 익히고 이해를 돕기 위해 학습 테스트를 작성하자.
- 오류가 발견될 경우 그에 대한 버그 테스트를 만들어두면 유용하다.
