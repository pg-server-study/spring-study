# 9.3장 애플리케이션 아키텍처

9.3장만 정리 합니다.

---

## 9.3 애플리케이션 아키텍처

스프링 웹 애플리케이션의 아키텍처를 알아보자

아키텍처는 여러 가지 방식으로 정의되고 이해될 수  있는 용어다.

가장 단순한 정의를 보자면 어떤 경계 안에 있는 내부 구성요소들이 어떤 책임을 갖고 있고,

어떤 방식으로 서로 관계를 맺고 동작하는지를 규정하는 것이라고 할 수 있다.

### 9.3.1 계층형 아키텍처

성격이 다른 모듈이 강하게 결합되어 한데 모여 있으면 한 가지 이유로 변경이 일어날 때

다른 요소도 함께 영향을 받게된다. 이런 부분을 방지 하기 위해서 인터페이스와 같은

유연한 경계를 만들어두고 분리하거나 모아두는 작업이 필요하다.

### 아키텍처와 SoC

지금까지는 주로 오브젝트 레벨에서 이런 분리의 문제에 대해 생각해봤다.

얽혀 있는 것을 분리하고 인터페이스를 둬서 유연하게 만들엇다.

이런 원리는 아키텍처 레벨에서 좀 더 큰 단위에 대해서도 동일하게 적용할 수 있다.

오브젝트를 하나의 모듈 단위라고 생각해보자.

모듈의 단위를 크게 확장해 볼 수도 있다.

애플리케이션을 구성하는 오브젝트들을 비슷한 성격과 책임을 가진 것들끼리 묶을 수 있다.

예를 들면 데이터 액세스 로직을 담당하는 DAO들은 하나의 단위로 생각해도 좋다.

어떤 DAO든 비슷한 성격을 띠고 유사한 방식으로 다른 오브젝트와 관계를 갖는다.

주로 DAO들은 저장과 검색등을 하고 DB의 엔티티 모델과 유사한 도메인 오브젝트를 이용해 데이터를

저장하고 가공하고 주고받는 일을 한다. 이는 각 DAO들의 유사한 성격이다

이렇게 애플리케이션 오브젝트들을 유사한 성격을 띤 그룹으로 나눌수가 있다.

만약 나뉘어 있지 않다면 DB 와 연동하는 DAO 코드에 HTML 태그에 들어가는 값이라던가

다양한 연산을 하는 로직이 섞여 있을 수 있다.

코드가 섞여 있을때 HTML이나 연산로직에 변경이 있으면 여러가지 오브젝트들을 수정해야 한다.

그래서 성격이 다른 것은 아키텍처 레벨에서 분리해주는 게 좋다.

이렇게 분리된 각 오브젝트는 독자적으로 개발과 테스트가 가능해서 개발과 변경 작업이 모두 빨라 질 수 있다.

또 구현 방법이나 세부 로직이 변경되어도 서로 영향을 주지않고 변경될 수 있을만큼 유연해진다.

이렇게 책임과 성격이 다른 것을 크게 그룹으로 만들어 분리해두는 것을 아키텍처 차원에서는

계층형 아키텍처(layered architecture) 라고 부른다.

보통 웹 기반의 엔터프라이즈 애플리케이션은 일반적으로 세 개의 계층을 갖는다고 해서

3계층 애플리케이션이라고 한다.

### 3계층 아키텍처와 수직 계층

3계층 아키텍처는 백엔드의 DB나 레거시 시스템과 연동하는 인터페이스 역할을 하는

데이터 액세스 계층, 비즈니스 로직을 담고 있는 서비스 계층, 주로 웹 기반의 UI를 만들어내고

그 흐름을 관리하는 프레젠테이션 계층으로 구분한다.

![Untitled](image/Untitled.png)

**데이터 액세스 계층**

데이터 액세스 계층은 DAO 계층이라고도 불린다.

DAO 패턴을 보편적으로 사용하기 때문이다.

또한 DB외에도 ERP, 레거시 시스템, 메인 프레임 등에 접근하는 역할을 하기 때문이

EIS 계층이라고도 한다.

보통은 장기적인 데이터 저장을 목적으로 한다.

또 외부 시스템을 호출해서 서비스를 이용하는 것은 기반(infrastructure) 계층으로 따로 분루햔다.

데이터 액세스 계층은 사용 기술에 따라서 다시 세분화된 계층으로 구분될 수 있다.

3계층과 달리 데이터 액세스 계층 안에서 다시 세분화하는 경우는 추상화 수준에 따른 구분이기 때문에

수직적인 계층이라고 부르기도 한다.

기본 3계층과는 다르게 역할에 따라 구분한 것이므로 보통 그림으로 나타낼 때도 가로로 배열한다.

반면에 같은 책임을 가졌지만 추상화 레벨에 따라 구분하는 경우는 세로로 배열해서 표현한다.

아래는 스프링의 JdbcTemplate을 사용하는 DAO 계층을 그린 그림이다.

![Untitled](image/Untitled%201.png)

JdbcTemplate 말고 다른것을 사용할때 이름 묶어서 더 단순한 방법으로

DAO 코드를 작성하고 싶다면 하나의 추상 계층을 추가할 수 있다.

![Untitled](image/Untitled%202.png)

이렇게 계층을 추가하면 개발자의 코드에 지대한 영향을 주기 때문에 매우 신중하게 결정해야 한다.

또한 유연하게 하위 계층의 변화에 대항할 수 있도록 변화에 대한 책임같은 설계를 잘해야하며

사용법에 대한 가이드라인이나 코딩 정책이 잘 만들어져서 개발자에게 제공될 필요가 있다.

**서비스 계층**

서비스 계층은 구조로 보자면 가장 단순하다.

스프링 애플리케이션의 서비스 계층 클래스는 이상적인 POJO로 작성된다.

서비스 계층은 DAO 계층을 호출 하고 이를 활용해서 만들어진다.

때론 데이터 액세스를 위한 기능 외에 서버나 시스템 레벨에서 제공하는

기반 서비스를 활용할 필요도 잇다.

예를 들어 웹 서비스와 같은 원격 호출을 통해 정보를 가져오거나

메일 또는 메시징 서비스를 이용하는 것이 대표적인 예다

이런 기반 서비스는 3계층 어디에서나 접근이 가능하도록 만들 수도 있고

아키텍처를 설계하기에 따라서 반드시 서비스 계층을 통해 사용되도록 제한할 수도 있다.

서비스 계층은 특별한 경우가 아니라면 추상화 수직 계층 구조를 가질 필요가 없다.

기술API 를 직접 다루는 코드가 아니기 때문에 기술에 일관된 방식으로

접근하게 하거나 편하게 사용하게 해주는 추상화는 필요 없기 때문이다.

아래는 서비스 계층이 기반 서비스 계층을 호출하는 것과

기반 서비스 계층이 서비스 계층을 호출하는 관계도이다

원칙적으로 서비스 계층 코드가 기반 서비스 계층의 구현에 종속되면 안된다.

굳이 필요하다면 AOP를 통해서 서비스 계층의 코드를 침범하지 않고

부가기능을 추가하는 방법을 활용해야 한다.

![Untitled](image/Untitled%203.png)

**프레젠테이션 계층**

프레젠 테이션 계층은 가장 복잡한 계층이다

웹과 프레젠테이션 기술은 끊임없이 발전하고 진보하고 새로운 모델이 등장하기 때문이다.

따라서 프레젠테이션 계층에서 사용 되야 할 기술과 구조를 선택하는 일은 간단하지 않다.

엔터프라이즈 애플리케이션의 프레젠테이션 계층은 클라이언트의 종류와 상관없이

HTTP 프로토콜을 사용하는 서블릿이 바탕이 된다

단순한 HTML 과 자바스크립트만을 사용하는 브라우저이든

다운로드해서 플러그인 안에서 동작하는 액티브X

RESTful 스타일이든

주로 HTTP 프로토콜을 선호한다.

클라이언트와 연결돼서 동작하는 프레젠테이션 계층은 자바에서는 주로

HTTP 프로토콜을 처리하는 가장 기본엔진인 서블릿으로 사용한다.

**계층형 아키텍처 설계의 원칙**

- 각 계층은 자신의 계층에 책임에만 충실해야 한다.
- 데이터 액세스 계층은 데이터 액세스에 관한 모든 것을 스스로 처리해야 한다.
- 프레젠테이션 계층의 오브젝트를 그대로 서비스 계층으로 전달하지 말자

  HttpServletRequest 나 HttpServletResponse , HttpSession 같은 타입을 서비스 계층

  메소드의 파라미터로 사요하면 안된다. ( 종속되지 않은 오브젝트로 변환해줘야 한다 )


---

### 9.3.2 애플리케이션 정보 아키텍처

엔터프라이즈 시스템은 본질적으로 동시에 많은 작업이 빠르게 수행돼야 하는 시스템이다.

엔터프라이즈 애플리케이션은 일반적으로 사용자의

요청을 처리하는 동안만 간단한 상태를 유지한다.

주요 상태정보는 클라이언트나 백엔드 시스템에 분산돼서 보관되며

이렇게 애플리케이션 사이에 두고 흘러다니는 정보를 어떤식으로 다룰지를

결정하는 일도 아키텍처를 결정할 때 매우 중요한 기준이 된다.

**DB / SQL**

데이터 중심 구조의 특징은 하나의 업무 트랜잭션에

모든 계층의 코드가 종속되는 경향이 있다는 점이다.

예를 들어 사용자 이름으로 사용자 저보를 검색해서 일치하는 사용자의 아이디,비밀번호 등을 보여주는

작업이 있다고 하면 이것이 하나의 업무 단위가 되면서 모든 계층의 코드가 이 기준에 맞춰서 만들어진다.

검색 조건은 SQL 로 만들어진다. 사용자 정보를 웹페이지에서 연도만 보여줘야한다면

SQL로 연도만 출력할 것이다 결국 SQL 의 결과로 이미 화면에 어떤 식으로 출력이 될지 알고있는 셈이다.

SQL의 결과물이 오브젝트에 저장돼서 전달된다.

DB 에서 모든것을 처리 했으니 서비스 계층은 별로 할일이 없다.

만약에 DB 컬럼이 변경되면 그에 맞게 뷰의 내용도 변경된다

모든 계층의 코드는 '이름을 이용한 고객 조회' 라는 업무에 종속된다.

또한 업무의 내용이 바뀌면 모든 계층의 코드가 함께 변경된다.

모든 계층의 코드가 종속적일 뿐 아니라 배타적이여서 다른 업무에 재사용되기 힘들다.

유사한 방법의 사용자 DAO 메소드라도 화면에 나타날 정보가 다르면 SQL이 달라지기에 새로 만들어야 한다.

![Untitled](image/Untitled%204.png)

이런 식의 개발 방법과 아키텍처는 사실 자바 기술이 발전하기 이전에 엔터프라이즈

시스템에서 흔히 발견할 수 있다. 요즘은 이것을 레거시로 많이 취급한다.

이런식의 개발 방식은 처음에는 쉽지만 여러가지 상황이 만들어지면 코드도 많아지고

관리하기도 힘들어진다. 그리고 단지 익숙하고 편하다는 이유로 스프링 애플리케이션 개발에도

여전히 DB 중심의 아키텍처를 선택한다면 스프링의 장점을 제대로 누릴 수 있는 기회를 얻지 못한다.

**거대한 서비스 계층 방식**

DB에서 가져온 데이터가 애플리케이션에 흘러다니는 정보의 중심이 되는 아키텍처이다

하지만 DB에 많은 로직을 두는 개발 방법의 단점을 피하면서 애플리케이션 코드의 비중을 높이는 방법이다.

DB에 부하가 안걸리제 저장 프로시저의 사용을 자제하고 복잡한 SQL을 피하고

주요 로직은 서비스 계층에서 코드로 처리하도록 하는 것이다.

이전 DB 중심 개발 방식보다 애플리케이션 코드의 비중이 커지고

그만큼 객체지향 개발의 장점을 살릴 기회가 많아진다.

또한 DB에서 연산하던걸 어플리케이션 코드에서 연산하면서

그중 일부는 여러 서비스 계층 코드에서 재사용이 가능해진다.

물론 비즈니스가 커지면 서비스 계층의 로직도 중복되고 커질수도 있다.

대부분의 비즈니스 로직이 서비스 계층 코드에 집중되기 때문이다.

![Untitled](image/Untitled%205.png)

---

### 9.3.3 오브젝트 중심 아키텍처

오브젝트 중심 아키텍처가 데이터 중심 아키텍처와 다른 가장 큰 특징은

도메인 모델을 반영하는 오브젝트 구조를 만들어두고 그것을 각 계층 사이에서

정보를 전송하는데 사용한다는 것이다.

**데이터와 오브젝트**

전형적인 1:N 관계다

![Untitled](image/Untitled%206.png)

이 구조를 도메인 오브젝트 로 나타내보자

```java
class Category {
	
	int categoryId;
	String description;
	List<Product> products;
}

class Product {
	int productId;
	String name;
	int price;
	Category category;
}
```

이 구조는 단순히 특정 SQL에 대응되는 맵과 배열, 매번 달라지는 SQL을 담을 오브젝트와 달리

애플리케이션 어디에도 사용될 수 있는 일관된 형식의 도메인 정보를 담고 있다.

도메인 모델을 반영하는 오브젝트를 사용하면 자바 언어의 특성을 최대한 활용할 수 있도록 정보를 가공할 수 있다.

대표적으로 오브젝트 사이의 관계를 나타내는 방법을 들 수있다.

자바에서 관계하고 있는 다른 오브젝트와 직접 연결하여

레퍼런스 변수를 이용해서 다른 오브젝트를 참조할 수도 있다.

이렇게 도메인 모델을 따르는 오브젝트 구조를 만들려면 DB에서 가져온 데이터를

도메인 오브젝트 구조에 맞게 변환해줄 필요가 있다. 한번 변환되면 그 이후의

작업은 수월해진다. DAO는 자신이 가져온 데이터를 오브젝트에 담아주고

서비스 계층에서는 이를 반환할 데이터에 맞게 가공하여 프레젠테이션 계층에

보내주면된다.

**도메인 오브젝트 사용의 문제점**

최적화된 SQL 매번 만들어 사용하는 경우에 비해 성능 면에서 조금 손해를 감수해야 할 수도 있다.

DAO는 비즈니스 로직의 사용 방식을 알지 못하므로 모든 필드 값을 채워 도메인 오브젝트로 보내준다.

오브젝트 관계에도 문제가 있다. 만약 단순히 Product 정보만 필요한 비즈니스 로직인데

DAO 가 준 오브젝트에는 관계를 가진 Category 오브젝트도 함께 담겨 있을 것이다.

또는 2개의 DAO를 만들어서 매번 다른 도메인 오브젝트를 반환하도록 할 수도 있다.

이런 문제를 해결하는 접근 방법은 여러 가지가 있다.

지연된 로딩 기법을 이용하면 일단 최소한의 오브젝트 정보만 읽어두고

관계하고 있는 오브젝트가 필요한 경우에만 다이내믹하게 DB에서 다시 읽어 올 수 있다.

추가로 가장 이상적인 방법은 JPA나 JDO, 하이버 네이트 같은

오브젝트 매핑 기술인 ORM기술을 사용 하는 것이다.

이런 ORM은 지연 로딩 기법을 편리하게 제공해준다

그래서 도메인 오브젝트 중심 아키텍처를 이용해 개발한다면

ORM과 같은 오브젝트 중심 데이터 액세스 기술을 사용하자.

**빈약한 도메인 오브젝트 방식**

도메인 오브젝트에 정보만 담겨 있고, 정보를 활용한 아무런 기능도 갖고 있지 않다면

이는 온전한 오브젝트라고 보기 힘들다. 이런 오브젝트를 빈약한 오브젝트 라고 부른다.

도메인 오브젝트는 데이터를 저장해두는 것 외에는 아무런 기능이 없다

도메인 오브젝트에 넣을 수 있는 기능은 어떤 것일까?

기능이라고 하면 그 도메인의 비즈니스 로직이라고 불 수 있다

그럼 빈약한 도메인 오브젝트의 방식에서는 비즈니스 로직이 어디에 존재할까?

바로 서비스 계청이다. 아래의 그림은 빈약한 도메인 오브젝트의 방식을 나타내고 있다.

![Untitled](image/Untitled%207.png)

**풍성한 도메인 오브젝트 방식**

풍성한 도메인 오브젝트는 빈약한 도메인 오브젝트의 단점을 극복하고

객체지향적인 특징을 잘 사용할 수 있도록 개선한 것이다.

아래와 같은 로직이 존재하는것이다 이 로직을 서비스 계층에 만들지않고

도메인 오브젝트의 메소드로 넣는것이다

```java
class Category {
	List<Product> products;
	.../

	public int calcTotalOfProductPrice() {
		int sum = 0;

		for( Product prd : this.products() }
			sum += prd.getPrice();
		}
		return sum;
	}
	
}
```

이렇게 도메인 오브젝트 안에 로직을 담아두면

이 로직을 서비스 계층의 메소드에 따로 만드는 경우보다 응집도가 높다.

**도메인 계층 방식**

도메인 오브젝트에 담을 수 있는 비즈니스 로직은 데이터 액세스 계층에서 가져온

내부 데이터를 분석하거나 분석하거나, 조건에따라 오브젝트 정보를 변경 생성하는 정도에 그칠 수 밖에 없다.

이렇게 변경된 데이터가 다시 DB에 적용되려면 서비스 계층 오브젝트의 부가적인 작업이 필요하다

도메인 오브젝트가 스스로 필요한 정보는 DAO를 통해 가져오고

생성이나 변경작업이 일어났을때 DAO에게 변경사항을 요청해달라고 할수는 없을까?

도메인 계츠으이 역할과 비중을 극대화 하려다 보면 기존의 풍성한

도메인 오브젝트 방식으로는 만족할 수 없다.  그래서 등장한 것이 바로

도메인 오브젝트가 기존 3계층과 같은 레벨로 격상되어 하나의 계층을 이루게 하는 도메인 계층 방식이다.

개념은 간단하다. 도메인 오브젝트들이 하나의 독립적인 계층을 이뤄서

서비스 계층과 데이터 액세스 계층의 사이에 존재하게 하는 것이다.

도메인 오브젝트가 독립적된 계층을 이뤘기 때문에 기존 방식과는 다른 두 가지 특징을 갖게 된다.

첫째는 도메인 종속적인 비즈니스 로직의 처리는 서비스 계층이 아닌

도메인 계츠으이 오브젝트 안에서 진행된다는 점이다.

데이터 액세스 계층을 통해 오브젝트를 가져왔든 상관없이

도메인 오브젝트에게 비즈니스 로직의 처리를 요청할 수 있다.

해당 도메인 오브젝트 중심으로 만들어진 로직이라면

그 이후 작업은 오브젝트가 직접 대부분의 비즈니스 로직을 처리할 수 있다.

두번째는 도메인 오브젝트가 기존 데이터 액세스 계층이나 기반 계층의 기능을 직접 활용할 수 있다는 것이다.

그런데 앞에서 도메인 오브젝트는 스프링에 등록돼서 싱글톤으로 관리되는 빈이 아니기 때문에

다른 빈을 DI 받을 수 없다고 했다.

그렇다면 도메인 오브젝트는 어떻게 다른 빈을 이용 할 수 있을까?

물론 방법은 DI다. 여전히 도메인 오브젝트는 스프링이 직접 만들고 관리하는 오브젝트

즉, 빈이아니다. 하지만 이런 스프링이 관리하지 않는 오브젝트에도 DI를 적용할 수 있다.

물론 그에 따른 간단한 설정이 추가돼야 한다.

스프링이 관리하지 않는 도메인 오브젝트에 DI를 적용하기 위해서는 AOP가 필요하다.

물론 스프링 AOP는 부가기능을 추가할 수 있는 위치가 메소드 호출과정으로 한정되고

AOP 적용 대상도 스프링의 빈 오브젝트 뿐이다.

하지만 스프링의 AOP 대신 AspecJ AOP를 사용하면 클래스의 생성자가 호출되면서

오브젝트가 만들어지는 시점을 조인포인트로 사용할 수 있고 스프링 빈이 아닌 일반 오브젝트에도

AOP 부가기능을 적용할 수 있다.

도메인 오브젝트를 독립적인 계층으로 만들려고 할 때 고려해야할 사항이 있다.

- 도메인 오브젝트가 도메인 계층을 벗어나서도 사용되게 할지 말지

  도메인오브젝트는 DB나 백엔드 시스템에 작업결과를 반영할 수 있다.

  이 오브젝트가 JSP같은 뷰로 갔다가 개발자가 해당 메소드를 실행하면 심각한 문제가 일어날 수 있다.


이런 문제를 피하려면 어떻게 해야 할까?

철저한 개발 가이드 라인을 만들어두고 이를 강력하게 적용하는 것이다.

또는 도메인 오브젝트는 도메인 계층을 벗어나지 못하게 하는 것이다

도메인 밖으로 전달 될때는 DTO라는 오브젝트를 만들어 전달한다

DTO는 Data Transfer Object라고도 불린다

![Untitled](image/Untitled%208.png)

---

## 9.4 정리

- 스프링은 어떤 플랫폼에서도 사용될 수 있지만, 기본적으로는 자바 엔터프라이즈 플랫폼에 최적화 되어 있다. HTTP를 통해 접근하는 웹 클라이언트와 백엔드 DB를 사용하는 애플리케이션에 적합하다.
- 스프링 애플리케이션은 역할에 따라 3계층으로 구분되고 다시 기술의 추상도에 따라 세분화되는 계층형 아키텍처를 사용하는 것이 좋다.
- 아키텍처는 애플리케이션이 다루는 정보의 관섬에서 데이터 중심과 오브젝트 중심으로 구분할 수 있다
- 스프링에 가장 어울리는 아키텍처는 오브젝트 중심의 아키텍처다.
- 스프링이 직접 지원하지 않는 서드파티 기술도 스프링 스타일의 접근 방식을 따라서 사용할 수 있도록 준비해둬야 한다.