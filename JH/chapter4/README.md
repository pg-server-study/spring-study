[노션을 참고하면 더욱 좋습니다.](https://near-sunscreen-c35.notion.site/4-f7c7cae35426438e9abcaa80ca1142bb)

# 4장 예외

이번 장에서는 JdbcTemplate를 통해서 예외 처리하는 법을 살펴본다.



## 4.1 사라진 SQLException

3장에서 달라진 deleteAll()

```
// 변경전
public void deleteAll() throws SQLException {
	this.jdbcContext.executeSql("delete from users");
}

// 변경후
public void deleteAll() {
	this.jdbcTemplate.update("delete from users");
}
```

JdbcTemplate를 사용한뒤 SQLException 이 사라졌다.

### 4.1.1 초난감 예외처리

예외 블랙홀 자바의 기초를 배운학생들이나 귀찮다고 대충

만드는 사람들이 많이 저지르는 악습니다.

```
// 1번
try {

} catch (SQLException e ) {
	// 아무것도 없음
}

// 2번

try {

} catch (SQLException e ) {
	System.out.println(e);
}

//3버ㅏㄴ
try {

} catch (SQLException e ) {
	e.printStackTrace();
}

```

위 소스들의 공통점

- 프로덕션에서는 에러가 발생해도 개발자가 에러를 확인할 수 없다 (에러를 확인하려면 24시간 모니터링을 해야한다. )
- 에러를 출력할 뿐이지 아무것도 하지 않는다.

모니터링 할 수 있는 특수한 동작을 시키는게 올바르다.

**무의미하고 무책임한 throws**

```
public void method1() throws Exception {
	method2();
}

public void method2() throws Exception {
	method3();
}

public void method3() throws Exception {
	//
}
```

이렇게 무의식적으로 throws Exception 을 던지는 코드도 볼수있다.

이런 두가지 습관은 어떤 경우에도 용납하지 않아야 한다.

### 4.1.2 예외의 종류와 특징

그렇다면 예외를 어떻게 다뤄야 할까?

가장 큰 이슈는 체크 예외 라고 불리는 명시적인 처리가 필요한 예외를 사용하고 다루는 방법이다.

자바에서 throw 를 통해 발생시킬 수 있는 예외는 크게 세 가지가 있따.

**Error**

첫째는 java.lang.Error 클래스의 서브클래스들이다. 에러는 시스템에 뭔가 비정상적인 상황이 발생했을 경우에 사용된다. 그래서 주로 자바 VM에서 발생시키는 것이고 애플리케이션 코드에서 잡으려고 하면 안된다.

**Exception 과 체크 예외**

java.lang.Exception 클래스와 그 서브클래스로 정의되는 예외들은 에러와 달리 개발자들이 만든 애플리케이션 코드의 작업 중에 예외상황이 발생했을 경우에 사용 된다.

Exception 클래스는 다시 체크예외와 언체크예외로 구분된다.

전자는 Exception 클래스의 서브클래스이면서 RuntimeException 클래스를 상속하지 않은 것들이고,

후자는 RuntimeException을 상속한 클래스를 말한다.

일반적으로 예외라고 하면 Exception 클래스의 서브클래스 중에서

**RuntimeException 을 상속하지 않은 것**만을 말하는 **체크 예외** 라고 생각해도 된다.

java.lang.RuntimeException 클래스를 상속한 예외들은 명시적으로 예외처리를 강제하지

않기 때문에 언체크 예외라고 불린다. 또는 대표 클래스 이름을 따서 런타임 에외 라고도 한다

에러와 마찬가지로 이 런타임 예외는 catch 문으로 잡거나 throws로 선언하지 않아도 된다.

물론 명시적으로 잡거나 throws로 선언해줘도 상관없다.

RuntimeException 을 상속받는 주요 에러들

- NullPointerException
- IllegalArgumentException
- ETC

### 4.1.3  예외처리 방법

첫번쨰로는 MAX_RETRY 만큼 재시도를 하는거다

```

int maxRetry = MAX_RETRY;

while(maxRetry -- > 0 ) {
  /// 반복
}

throw new RetryFailedException(); // 예외발생

```

두번쨰 예외처리 회피

두번쨰 방법은 throws 문으로 선언해서 발생하면 던지는거다.

```
public void add() throws SQLException {
		// JDBC API
}
```

마지막으로 예외 전환을 하는것이다.

의미있는 예외를 던져 왜 에러가 발생했는지 알기 쉽게 한다.

```
public void add(User user) throws DuplicateUserIdException, SQLException {
		try {
				// JDBC...
		} catch (SQLException e) {
				if(e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY)
					throw DuplicateUserIdException();
				else 
					throw e;
		}
}
```

이런식으로 의미있는 예외를 던져 무슨 이유때매 예외가 발생했는지 알기쉽게하여

처리하기 편하게한다.

### 4.1.4 예외처리 전략

add() 메소드의 예외처리

위에서 만든 DuplicateUserIdException 을 굳이 체크 예외로 만들지 않고 런타임 예외로 만들자.

RuntimeException 을 상속한 예외 처리 클래스를 만들자

```java
public class DuplicateUserIdException extends RuntimeException {

    public DuplicateUserIdException(Throwable cause) {
        super(cause);
    }
}
```

이제 add() 메소드를 수정하자.

```
public void add(final User user) throws DuplicateUserIdException {
     try {
         this.jdbcTemplate.update("insert into users(id, name, password) values(?,?,?)", user.getId(), user.getName(), user.getPassword());
     } catch (SQLException e) {
         if(e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY) {
             throw new DuplicateUserIdException(e);
         } else {
             throw new RuntimeException(e);
         }
     }
 }
```

런타임 예외를 사용하는 경우에는 API 문서나 레퍼런스 문서 등을 통해, 메소드를 사용할 때 발생할

수 있는 예외의 종류와 원인, 활용 방법을 자세히 설명해두자.

### 4.1.5 SQLException은 어떻게 됐나?

지금까지 다룬 예외처리에 대한 애용중 SQLException 선언이 사라졌을것이다.

먼저 생각해볼것이 있따 SQLException은 과연 복구가 가능한 예외인가?

99%는 복구가 불가능하다. 통제할 수 없는 외부상황 떄문에 발생한다

- SQL 문법이 틀렸거나.
- 제약조건을 위반했거나
- DB서버가 다운됐다거나
- 네트워크가 불안정하거나
- DB 커넥션 풀이 꽉찬경우나

시스템 예외라면 애플리케이션 레벨에서 복구할 방법이없다.

관리자나 개발자에게 예외가 발생했따는 사실이 알려지도록 전달하는 방법밖에는 없다.

위에서 설명했듯 대부분의 SQLException은 복구가 불가능하다.

그렇다면 예외처리 전략을 적용해야 한다.

필됴오 없는 기계적인 throws 선언이 등장하도록 방치하지 말고 가능한 빨리 언체크/런타임 예외로

전환해야 한다.

스프링의 JdbcTemplate은 바로 이 예외처리 전략을 따르고 있다.

템플릿과 콜백 안에서 발생하는 모든 SQLException을 런타임 예외인 DataAccessException으로 포장해서 던져준다. 따라서 JDbcTemplate을 사용하는 UserDao 메소드에선

꼭 필요한 경우에만 런타임 예외인 DataAccessException을 잡아서 처리하면 된다.



## 4.2 예외 전환

### 4.2.1 JDBC의 한계

DB를 이용하는데 하나의 DB를 쓰지않고 여러 DB를 쓴다고 생각해보자.

각 DB별로 API를 제공해야하며 DB가 바뀔때마다 코드도 바뀐다

또한 우리가 위에서 만든 ID가 중복되는 에러도 Mysql의 코드를 사용한거지

오라클이나 다른 DB로 가면 코드가 변할수도 있다.

### 4.2.2 DB 에러 코드 매핑을 통한 전환

DB 종류가 바뀌더라도 DAO를 수정하지 않으려면 이 두가지 문제를 해결해야 한다.

SQL과 관련된부분은 뒤에서 다루고 여기서는 SQLException의 비표준 에러코드와

SQL 상태정보에 대한 해결책을 알아보자.

SQLException에 담긴 SQL 상태코드는 신뢰할 만한 게 아니므로 더이상 고려하지 않는다.

차라리 DB 업체별로 만들어 유지해오고 있는 DB 전용 에러 코드가 더 정확한 정보라고 볼 수 있다.

해결방법은 DB별 에러 코드를 참고해서 발생한 예외의 원인이 무엇인지 해석해주는 기능을 만드는 것이다.

키값에 중복에 대해서 MySQL 이라면 1062, 오라클이라면 -803 에러 코드를 받는다.

이런 에러 코드 값을 확인할 수 있따면, 키 중복 때문에 발생하는 SQLException을 DuplicateKeyException이라는 의미가 분명히 드러나는 예외로 전환할 수 있다.

스프링에서는 DB별 에러 코드를 분류해서 스프링이 정의한 예외클래스와 매핑할수 있는 테이블을

만들어두고 이를 이용한다.

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter4/image/Untitled.png?raw=true)

이런식으로 DataAccessException의 서브클래스 타입으로 정의해서 이용한다.

JdbcTemplate 안에서 DB별로 준비된 에러코드와 비교해서 적절한 에러를 던져주기 때문에

JdbcTemplate을 이용한다면 DB 관련 예외는 거의 신경 쓰지 않아도 된다.

### 4.2.3 DAO 인터페이스와 DataAccessException 계층 구조

DataAccessException은 JDBC의 SQLException을 전환하는 용도로만 만들어진 건 아니다.

JDBC 외의 자바 데이터 액세스 기술에서 발생하는 예외에도 적용된다.

자바에는 JDBC 외에도 데이터 액세스를 위한 표준 기술이 존재한다.

JDO나 JPA는 JDBC 와 마찬가지로 자바의 표준 퍼시스턴스 기술이지만 JDBC와는 성격과 사용 방법이 크게 다르다.

DataAccessException은 의미가 같은 예외라면 데이터 액세스 기술의 종류와 상관없이 일관된 예외가 발생하도록 만들어준다. 데이터 액세스 기술에 독립적인 추상화된 예외를 제공하는 것이다.

스프링이 왜 이렇게 DataAccessException 계층 구조를 이용해 기술에 독립적인 예외를 정의하고

사용하게 하는지 생각해보자.

**DAO 인터페이스와 구현의 분리**

기술에 독립적인 이상적인 DAO 인터페이스

```java
public interface UserDao {
		public void add(User user);
}
```

위처럼 선언이 가능할까?

가능하지 않다. DAO 에서 사용하는 데이터 액세스 기술의 API가 예외를 던지기 떄문이다.

각자 독자적인 예외를 던지기 때문에 불가능하다.

```java
public interface UserDao {
		public void add(User user) throws SQLException; // JDBC API
		public void add(User user) throws PersistentException; // JPA
		public void add(User user) throws HibernateException; // Hibernate
		public void add(User user) throws JdoException; // JDO
}
```

가장 간단한 방법으로 해결해보자

```java
public interface UserDao {
		public void add(User user) throws Exception;
}
```

이러면 가능하다 하지만 무책임한 선언이다..

다행히도 JDBC보다는 늦게 등장한 JDO, Hibernate, JPA 등의 기술은 SQLException 같은

체크 예외 대신 런타임 예외를 사용한다

따라서 throws 에 선언을 해주지 않아도 된다.

남은 것은 SQLException을 던지는 JDBC API를 직접 사용하는 DAO 뿐인데.

이 경우에는 DAO 메소드 내에서 런타임 예외로 포장해서 던져줄 수 있다.

JDBC를 이용한 DAO에서 모든 SQLException을 런타임 예외로 포장해주기만 한다면

DAO의 메소드는 처음 의도한대로 선언해도된다.

```java
public interface UserDao {
		public void add(User user);
}
```

하지만 이것만으로는 불충분하다.

추후 11장에서 DataAccessException의 예외 사용법에 대해 자세히 살펴보고 정리해 보겠다.

## 4.3 정리

- 예외를 잡아서 아무런 조취를 취하지 않거나 의미 없는 throws 선언을 남발하는 것은 위험하다.
- 예외는 복구하거나 예외처리 오브젝트로 의도적으로 전달하거나 적절한 예외로 전환해야 한다.
- 좀 더 의미 있는 예외로 변경하거나, 불필요한 catch/throws 를 피하기 위해 런타임 예외로 포장하는 두 가지 방법의 예외 전환이 있다.
- 복구할 수 없는 예외는 가능한 한 빨리 런타임 예외로 전환하는 것이 바람직하다.
- 애플리케이션의 로직을 담기 위한 예외는 체크 예외로 만든다.
- JDBC의 SQLException은 대부분 복구할 수 없는 예외이므로 런타임 예외로 포장해야 한다.
- SQLException의 에러 코드는 DB에 종속되기 때문에 DB에 독립적인 예외로 전환될 필요가 있다.
- 스프링은 DataAccessException을 통해 DB에 독립적으로 적용 가능한 추상화된 런타임 예외 계층을 제공한다.
- DAO를 데이터 액세스 기술에서 독립시키려면 인터페이스 도입과 런타임 예외 전환, 기술에 독립적인 추상화된 예외로 전환이 필요하다.
