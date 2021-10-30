# 4장 예외

**4장에서는 JdbcTemplate을 대표로 하는 스프링의 데이터 액세스 기능에 담겨 있는 예외처리와 관련된 접근 방법에 대해 알아본다 . 이를 통해 예외를 처리하는 베스트 프랙티스도 살펴본다.**

## 4.1 사라진 SQLException

3장에서 JdbcContext로 만들었던 코드를 스프링의 JdbcTemplate을 적용하도록 바꾸면서 설명하지 않고 넘어간 부분이 있다. JdbcTemplate으로 바꾸기 전과 후의 deleteAll() 메소드를 비교해보고 무엇이 달라졌는지 찾아보자.

```java
//JdbcTemplate 적용전
public void deleteAll() throws SQLException {
	this.jdbcContext.excuteSql("delete from users");
}
//jdbcTemplate 적용후
public void deleteAll(){
	this.jdbcTemplate.update("delete from users");
}
```

JdbcTemplate 적용 이전에는 있었던 throws SQLException 선언이 적용 후에는 사라졌음을 알 수 있다. SQLException은 JDBC API 의 메소드들이 던지는 것이므로 당연히 있어야 한다. 그런데 변경 된 코드에서는 SQLException이 모두 사라졌다 . 과연 어디로 간것 일까 ?

### 4.1.1 초난감 예외처리

알아보기전에 개발자들의 코드에서 종종 발견되는 초난감 예외처리의 대표선수들을 알아보자 .

> 예외를 잡고는 아무것도 하지않는코드

```java
try {
	//내용생략
}catch(SQLExcepiton e){
}
```

예외가 발생하면 그것을 catch 블록을 써서 잡아내는 것까지는 좋은데 ,**아무 처리도 하지 않고 별문제 없이 넘어가 버린다면 프로그램 실행 중에 어디선가 오류가 발생 하였는데 그것을 무시하고 계속 진행해버린다. ** 결국 발생한 예외로 인해 어떤 기능이 비정상적으로 작동하거나 , 메모리나 리소스가 소진되거나 예상치못한 다른 문제를 일으킬 것이다. **더 큰 문제는 그 시스템 오류나 이상한 결과의 원인이 무엇인지 찾아내기 매우 힘들다는 점이다.**

> 예외가 잡고 메세지만 남기는 예외처리

```java
}catch(SQLExcepiton e){
	System.out.println(e);
}
}catch(SQLExcepiton e){
	e.printStackTrace();
}
```

위에 예외를 잡고 아무것도 하지않는 코드와 같이 마찬가지로 좋지 않다. 왜냐면 다른로그나 메세지에 금방 묻혀버리면 놓치기 쉽상이다 . 콘솔로그를 누군가 계속 모니터링 하지 않는 이상 이 예외 코드는 심각한 폭탄으로 남아있을 것이다.

**예외 처리를 할 때 반드시 지켜야할 핵심 원칙은 한 가지다 .** 모든 예외는 적절하게 복구되든지 아니면 작업을 중단시키고 운영자 혹은 개발자에게 분명하게 통보돼야 한다.

예외를 무시하고 정상적으로 동작하고 있는 것 처럼 모른척 다음 코드로 실행을 이어간다는 건 말이 되지 않는다.

> 무의미하고 무책임한 throws

```java
public void method1() throws Exception{
	method2();
	//생략
}
public void method2() throws Exception{
	method3();
	//생략
}
public void method3() throws Exception{
```

자신이 사용하려고 하는 메소드에 throws Exception이 선언되어 있다고 가정할 경우 , 그런 메소드 선언에서는 의미 있는 정보를 얻을수 없다. 정말 무엇인가 실행중에 예외적인 상황이 발생 할수 있다는 것인지 아니면 그냥 습관적으로 복사해서 붙여 놓은건지 알수가 없다.

결국 이런메소드를 사용하는 메소드에서도 역시 throws Exception을 따라서 붙이는 수 밖에 없다.**결과적으로 적절한 처리를 통해 복구 될수 있는 예외상황도 제대로 다룰수 있는 기회를 박탈당한다.**

## 4.1.2 예외의 종류와 특징

예외는 주로 자바에서 제공하는 java.lang.Exception 클래스와 Exception클래스의 서브 클래스들이 쓰이는 상황을 말한다.

예외는 체크예외와 언체크 예외 두가지로 나눌수 있는데. 언체크 예외는 RuntimeException 을 상속한것들을 말하고 , 체크예외는 이외의 예외들을 말한다.

**체크 예외**

RuntimeException을 상속하지 않는 예외들 . 체크 예외가 발생할수 있는 메소드를 사용할 경우 , 복구 가능한 예외들이기 때문에 반드시 예외를 처리하는 코드를 작성해야한다 . 이를 작성하지 않을시 컴파일 에러가 발생한다.

> ex ) IOException , SQLException 등

**언체크 예외**

RuntimeException을 상속한 예외들. 명시적으로 예외처리를 강제하지 않는다. 피할수 있지만 개발자가 부주의 해서 발생할 수 있는 경우에 발생하도록 만든것이다. 따라서 예상하지 못했던 예외 상황에서 발생하는게 아니기 때문에 꼭 catch나 throws를 사용하지 않아도 된다.

> ex) NullPointerException , IllegalArgumentException 등

### 4.1.3 예외처리 방법

첫번째 예외 처리 방법은 예외 상황을 파악하고 문제를 해결해서 정상 상태로 돌려놓는 것이다.

> 재시도를 통해 예외를 복구하는 코드 

```java
	int maxrettry = MXA_RETRY;
    
    while(maxretry --> 0){
        try {
            ... //예외가 발생이 가능성이 있는코드
            return;	//작업성공 
        }catch (SomeException e){
            //로그 출력 . 정해진 시간만큼 대기
        }finally {
            //리소스 반납. 정리작업
        }
    }
    throw new RetryFailedException();
```

두번째 방법은 예외처리를 자신이 담당하지 않고 자신을 호출한 쪽으로 던져버리는 것이다.

> 예외처리 회피

```java
public void add() throws SQLException {
	//JDBC API
}
```

SQLException을 자신이 처리하지 않고 템플릿으로 던져버린다. ( 위에 설명 했던 무책임한 예외처리 코드 )

마지막으로 예외를 처리하는 방법은 예외 전환을 하는것이다.

예외처리 회피와 비슷하게 예외를 복구해서 정상적인 상태로는 만들수 없기 때문에 예외를 메소드 밖으로 던지는 것이다. **예외회피와 달리 발생한 예외를 그대로 넘기는게아니라 적절한 예외로 전환해서 던진다.**

예외 전환은 보통 두가지 목적으로 사용된다.

첫째는 내부에서 발생한 예외를 그대로 던지는 것이 그 예외 상황에대한 적절한 의미를 부여해주지 못하는경우에 의미를 분명하게 해줄수 있는 예외로 바꿔주기 위해서다.

> 중복사용자가 있어서 db에러가 발생하면 Jdbc api는 SQLException을 발생시킨다.
>
> 이경우 DAO메소드가 SQLException을 그대로 밖으로 던져버리면 DAO를 이용해 사용자를 추가하려고 한 서비스 계층등에서는 왜 SQLException이 발생했는지 쉽게 알 방법이 없다.
>
> 로그인 아이디 중복 같은 경우는 충분히 예상 가능하고 복구 가능한 예외 상황이다.
>
> 이럴땐 DAO에서 SQLException의 정보를 해석해서 DuplicateUserIdException 같은 예외로 바꿔 던져주는게 좋다.

```java
public void add(User user) throws  DuplicateUserIdException,SQLException{
	try {
        //Jdbc를 이용해 user 정보를 db에 추가하는 코드 
        //또는 그런기능을 가진 다른 SQLException을 던지는 메소드를 호출하는 코드
	}catch (SQLException e){
        //ErrorCode가 MySQL의 "Duplicate Entry(1062)"면 예외 전환
        if(e.getErrorCode() == MySqlErrorNumbers.ER_DUP_ENTRY)
            throw DuplicateUserIdException();
        else 
            throw e;
        }
    }
}
```

보통 전환하는 예외에 원래 발생한 예외를 담아서 중첩 예외로 만드는것이 좋다 . getCause() 메소드를 이용해서 처음 발생한 예외가 무엇인지 확인할 수 있다.

```java
catch(SQLException e){
    ...
    throw DuplicateUserIdException(e);
}
```

````java
catch(SQLException e){
    ...
    throw DuplicateUserIdException().initCause(e);
}
````

두번째 전환 방법은 예외처리를 하기쉽고 단순하게 만들기 위해 포장하는 것이다.

중첩예외를 이용해 새로운 예외를 만들고 원인이 되는 예외를 내부에 담아서 던지는 방식은 같다. 하지만 의미를 명확하게 하려고 다른 예외로 전환하는 것이 아니라 **주로 예외처리를 강제하는 체크 예외를 언체크 예외인 런타임 예외로 바꾸는경우에 사용한다.**

```java
try{
	OrderHome orderHome = EJBHomeFactiory.getInstance().getOrderHome();
	Order order = orderHome.findByPrimaryKey(Integer id);
}catch(NamingException ne){
	throw new EJBException(ne);
}catch(NamingException ne){
	throw new EJBException(ne);
}catch(NamingException ne){
	throw new EJBException(ne);
}
```

EJBException는 RuntimeException 클래스를 상속한 런타임 예외다. 이렇게 런타임 예외로 만들어서 전달하면 EJB는 이를 시스템 익셉션으로 인식하고 트랜잭션을 자동으로 롤백해준다.

런타임 예외이기 때문에 잡아도 복구할만한 방법이 없어 일일이 예외를 잡거나 다시 던지를 수고를 할필요가없다.

반대로 애플리케이션 로직상에서 예외 조건이 발견되거나 예외상황이 발생 할수도 있다. 이런것은 api가 던지는 예외가 아니라 애플리케이션 코드에서 의도적으로 던지는 예외다. 이때는 체크 예외를 사용하는것이 적절하다. 비즈니스적인 의미가 있는 예외는 이에대한 적절한 대응이나 복구 작업이 필요하기 때문이다.

어차피 복구가 불가능한 예외라면 가능한 빨리 런타임 예외로 포장해 던지게 해서 다른 계층의 메소드를 작성할때 불필요한 throws선언이 들어가지 않도록 해줘야한다.

대부분 서버 환경에서는 애플리케이션 코드에서 처리하지 않고 전달된 예외들을 일괄적으로 다룰수 있는 기능을 제공한다.

## 4.1.4 예외처리 전략

### 런타임 예외의 보편화

> 앞서 add()메소드 에서는 두가지 체크 예외를 던지게 되어있는데 , 그 원인이 id중복일경우 DuplicateUserIdException으로 전환해주고 , 아니라면 SQLException을 그대로 던지게 했다. 
>
> DuplicateUserIdException은 충분히 복구 가능한 예외이므로 add()메소드를 사용하는쪽에서 잡아서 대응 할수 있지만 SQLException은 대부분 복구가 불가능한 예외 이므로 잡아봤자 처리할것도 없고 , 결국 throws를 타고 계속 앞으로 전달되다가 애플리케이션 밖으로 던져질것이다.
>
> DuplicateUserIdException처럼 의미 있는 예외는 add()메소드를 바로 호출한 오브젝트 대신 더 앞단에 오브젝트에서 다룰수도 있다, 어디에서든 DuplicateUserIdException을 잡아서 처리할수 있다면 굳이 체크 예외가 아니라 런타임 예외로 만드는것이 낫다. 그래야 add()메소드를 사용하는 코드를 만드는 개발자에게 의미 있는 정보를 전달해줄수있다.

```java
//아이디 중복시 사용하는 예외
public class DuplicateUserIdException extends RuntimeException{
	public DuplicateUserIdException(Throwable cause){
		super(cause);
	}
}
```

>Tip > Throwable 는 자바에서 예외 처리를 하기 위한 최상위 클래스이다.

```java
public void add(User user) throws  DuplicateUserIdException{
	try {
        //Jdbc를 이용해 user 정보를 db에 추가하는 코드 
        //또는 그런기능을 가진 다른 SQLException을 던지는 메소드를 호출하는 코드
	}catch (SQLException e){
        if(e.getErrorCode() == MySqlErrorNumbers.ER_DUP_ENTRY)
            throw DuplicateUserIdException(e); //예외전환
        else 
            throw new new RuntiomeException(e); //예외포장
        }
    }
}
```

이제 SQLException은 언체크 예외가 되었다. 따라서 메소드 선언에 throws의 포함시킬 필요가없다.

반면에 역시 언체크 예외로 만들어지긴 했지만 add()메소드를 사용하는 쪽에서 아이디 중복 예외를 처리 하고 싶을경우 활용 할수 있음을 알려주도록 DuplicateUserIdException을 메소드에 throws 선언에 포함시켰다. 

이제 이 add()메소드를 사용하는 오브젝트는 불필요한 throws 선언을 할 필요는 없으면서 , 필요한경우 DuplicateUserIdException을 이용할수 있다.

### 애플리케이션 예외

비즈니스 규칙을 위반했을 때 발생하는 예외(ID 중복오류, 재고 부족 등)로 분류되는 예외다. 이 오류가 발생하면 애플리케이션 요구사항에 정해져 있는 오류 처리 내용을 구현해야 한다. 

## 4.2 예외 전환

예외를 다른 것으로 바꿔서 던지는 예외 전환의 목적은 두 가지라고 설명했다. 하나는  앞에서 적용해본 것처럼 런타임 예외로 포장해서 굳이 필요하지 않은 catch/throws를  줄여주는 것이고， 다른 하나는 로우레벨의 예외를 좀 더 의미 있고 추상화된 예외로 바꿔서 던져주는것이다.

### jdbc의 한계

첫째 문제는 JDBC 코드에서 사용히는 SQL이다.SQL은 어느 정도 표준화된 언어이고  몇 가지 표준 규약이 있긴 하지만 대부분의 DB는 표준을 따르지 않는 비표준 문법과 기능도 제공한다.

해당 db의 특별한 기능을 사용하거나 최적화된 SQL을 사용할때 유용하기 때문이다 . 하지만 DB의 **변경가능성을 고려해서 우연하게 만들어야 한다면 SQL은 제법큰 걸림돌이 된다.**

두번째 문제는 DB마다 SQL만 다른게 아니라 에러 종류와 원인도 제각각이라는 점이다.

`if(e.getErrorCode() == MysqlErrorNumbers.ER_DUP_ENTRY)..`  같은 코드를 사용할 경우 . DB가 바뀐다면 에러 코드가 달라지므로 이코드는 기대한 대로 동작하지 못할것이다.

### 4.2.2 DB에러 코드 매핑을 통한 전환

스프링은 DB별 에러 코드를 분류해서 스프링이 정의한 예외 클래스와 매핑해놓은 에러 코드 매핑정보 테이블을 만들어두고 이를 이용한다.

드라이버나 DB메타정보를 참고해서 DB종류를 확인하고 DB별로 미리 준비된 매핑정보를 참고해서 적절한 예외 클래스를 선택하기 때문에 **DB가 달라져도 같은 종류의 에러라면 동일한 예외를 받을수 있다. **

DB 종류와 상관없이 중복키로 인해 발생하는 에러는 DuplicateKeyException으로 매핑돼서 던져진다.

```java
public void add() throws DuplicateKeyException{
	//jdbcTemplate을 이용해 user을 add하는 코드 
}
```

add() 메소드를 사용하는 쪽에서 중복키 상황에 대한 대응이 필요한 경우에 참고 할수 있도록 DuplicateKeyException을 메소드 선언에 넣어주면 편리하다.

### 4.2.3 DAO 인터페이스와 DataAccessException 계층구조

DataAccessException 은 의미가 같은 예외라면 데이터 엑세스 기술의 종류와 상관 없이 일관된 예외가 발생하도록 만들어준다 . **스프링이 왜 이렇게 DataAccessException 계층구조를 이용해 기술에 독립적인 예외를 정의하고 사용하게 하는지 생각해보자.**

### DAO 인터페이스와 구현의 분리 

DAO를 따로 만들어서 사용하는 이유는 데이터 엑세스 로직을 담은 코드를 성격이 다른 코드에서 분리해 놓기 위해서다 . 또한 분리된 DAO는 전략패턴을 적용해 구현 방법을 변경해서 사용할수 있게 만들기위해서 이기도하다. DAO를 사용하는 쪽에서는 DAO가 내부에서 어떤 데이터 액세스 기술을 사용하는지 신경 쓰지 않아도된다.

> UserDao의 인터페이스를 분리해서 기술에 독립적인 인터페이스로 만들려면 다음과같이 정의해야한다 .

```java
public interfave UserDao{
	public void add(User user); 
}
```

하지만 위의 메소드 선언을 사용할수없다 인터페이스의 메소드 선언에는 없는 예외를 구현 클래스 메소드에 throws에 넣을수는 없다.

```java
public void add(User user) throws SQLException;
public void add(User user) throws PersistentException; // JPA 
public void add(User user) throws HibernateException; // Hibernate
```

결국 인터페이스로 메소드의 구현은 추상화 했지만 구현 기술마다 던지는 예외가 다르기 때문에 메소드의 선언이 달라진다는 문제가 발생한다 . Dao 인터페이스를 기술에 완전히 독립적으로 만들려면 예외가 일치하지 않는 문제도 해결해야한다.

다행히 JDO , Hibernate , JPA 등의 기술은 체크예외 대신 런타임 예외를 사용하기 때문에 throws에 선언을 해주지 않아도 된다 . 남은것은 JDBC 를 사용하는 DAO뿐인데 , 이경우에는 DAO메소드 내에서 런타임 예외로 포장해서 던져줄수 있다.

**따라서 DAO를 사용하는 클라이언트 입장에서는 DAO의 사용 기술에 따라서 예외 처리 방법이 달라져야한다 . 결국 클라이어느가 DAO의 기술에 의존적이 될수 밖에없다.**

### 데이터 액세스 예외 추상화와 DataAccessException 계층구조

그래서 스프링은 자바의 다양한 데이터 액세스 기술을 사용할 때 발생하는 예외들을 추상화해서 DataAccessException 계층구조안에 정리해놓았다.

DataAccessException은 자바의 주요 데이터 액세스 기술에서 발생할 수 있는 대부분의 예외를 추상화 하고있다 . 데이터 액세스 기술에 상관없이 공통적인 예외도 있지만 일부 기술에서만 발생하는 예외도 있다 . orm에서는 발생하지만 jdbc에는 없는 예외가 있다 . **DataAccessException 은 이런 일부 기술에서만 공통적으로 나타나는 예외를 포함해서 데이터 액세스 기술에서 발생 가능한 대부분의 예외를 계층구조로 분류해놓았다.**

## 잠깐 ! 락킹이란 ?

> 책에는 나와 있는 락킹에대해서 처음들어봐서 별도로 락킹에 대해서 정리했습니다 !

**자원에대한 동시 요청이 발생 했을때 일관성에 문제가 발생할 수 있습니다 . 이를 방지하기 위해 자원에 대한 수정을 못하도록 사용하는게 락킹**

### 비관적 락 (Pessimistic Lock)

- 동시에 요청이 발생할경우 먼저온 요청에대한 자원을 선점해서 다른곳에서 접근하지 못하도록 막는것

- 접근할 때 무조건 잠그고 시작하기 때문에 선점락, 비관적 락이라는 용어를 사용.

### 낙관적 락 ( Optimistic Lock )

- 수정할때 내가먼저 수정했다고 명시하여 다른사람이 동일한 조건으로 값을 수정할수 없게하는것.
- DB에서 제공해주는 특징을 이용하는것이아닌 appilcation level에서 잡아주는 lock 이다.
- 트랜잭션 충돌이 발생하지 않는다는 가정을 함 
- 커밋하기 전까진 트랜잭션의 충돌을 알수없음

> 두 락의 큰 차이는 처리하는부분 비관적락은 DB단에서 처리 낙관적 락은appilcation단에서 처리한다 . 락킹처리가 필요한 부분에서 적절한 락을 사용해서 사용하면된다. 
>
> 자세한 성능 , 적절한 예시는 현재 필요한 내용보다 길어질수 있으므로 생략하겠다.

JDO , JPA , 하이버네이트처럼 오브젝트 / 엔티티 단위로 정보를 업데이트 하는 경우에는 낙관적 락킹이 발생할수 있는데 , 이런 예외들은 사용자에게 적절한 안내 메세지를 보여주고 , 다시 시도할수 있도록 해줘야한다 . 하지만 역시 **JDO , JPA , 하이버네이트마다 다른 종류의 낙관적인 락킹 예외를 발생시킨다. **그런데 스프링의 예외 전환 방법을 적용하면 기술에 상관없이 DataAccessException의 서브클래스인 ObjectQptimisticLockndFailureException으로 통일 시킬수 있다.

jdbcTempate과 같이 스프링의 데이터 액세스 지원 기술을 이용해 dao를 만들면 사용 기술에 독립적인 일관성있는 예외를 던질수 있다 . 

### 4.2.4 기술에 독립적인 UserDao 만들기 

지금 까지 만들어서 써왔던 UserDao 클래스 이제 인터페이스와 구현으로 분리해보자.

```java
public interface UserDao{
	void add(User user);
	User get(String id);
	List<User> getAll();
	void deleteAll();
	int getCount();
}
```

기존의 UserDao 클래스는 이름을 UserDaoJdbc로 변경하고 UserDao 인터페이스를 구현하도록 implements로 선언해준다.

```java
public class UserDaoJdbc implements UserDao{
}
```

스프링 설정파일의 userDao빈 클래스를 바꾸어준다.

```xml
<bean id="userDao" class="springbook.dao.UserDaoJdbc">
    	<property name="dataSource" ref="dataSource" />
</bean>
```

보통 빈의 이름은 구현인터페이스를 따르는 경우가 일반적이다 . 그래야 나중에 구현 클래스를 바꿔도 혼란이 없기때문이다.

### 테스트 보완

```java
public class UserDaoTst{
	@Autowried
	private UserDao dao; //UserDaoJdbc로 변경해야 하나?
}
```

일단 UserDao테스트는 DAO의 기능을 검증하는것이 목적이지 jDBC를 이용한 구현에 관심이 있는것이 아니다 . 그러니 UserDao라는 변수 타입을 그대로 두고 스프링 빈을 인터페이스로 가져 오도록 만드는 편이 낫다.

```java
@Test(expected=DataAccessExcetion.class)
public void duplciateKey(){
	dao.deleteAll();
	
	dao.add(user1);
	dao.add(user1); //예외발생 
}
```

테스트가 성공 할경우 DataAccessExcetion 타입의 예외가 던져졌음이 분명하다. DataAccessExcetion의 서브 클래스 일수도 있으므로 구체적으로 어떤 예외인지 확인해 볼 필요가있다. 이번엔 expected=DataAccessExcetion.class 부분을 빼고 테스트를 실행해보자.

테스트는 실패 했지만 DataAccessExcetion 의 서브 클래스인 DuplicteKeyException이 발생한것을 확인 할수있다.

### DataAccessExcetion 활용시 주의사항

안타깝게 DuplicteKeyException은 아직 까지는 Jdbc를 이용하는 경우에만 발생한다. 데이터 액세스 기술을 하이버네이트나 jpa를 사용했을때도 동일한 예외가 발생 할것으로 기대하지만 실제로 다른 예외가 던져진다.

그이유는 SQLException에 담긴 DB에러 코드를 바로 해석하는 JDBC의 경우와달리 JPA나 하이버네이트등에서는 각 기술이 재정의한 예외를 가져와 **스프링이 최종적으로 DataAccessExcetion 으로 변환하는데 , DB의 에러 코드와 달리 이런 예외들은 세분화되어 있지 않기 때문이다.**

기술에 상관없이 어느정도 추상화된 공통 예외로 변환해주긴하지만 근본적인 한계 때문에 완벽하다고 기대할 수는 없다. 따라서 사용에 주의를 기울여야한다.

DataAccessExcetion을 잡아서 처리하는 코드를 만들려고 한다면 미리 학습테스트를 만들어서 실제로 전환되는 예외의 종류들을 확인해둘필요가있다.

```java
public class UserDaoTest{
	@Autowried UserDao dao;
	@Autowride DataSource dataSource;
}
```

> DataSource를 사용해 SQLException에서 직접 DuplicteKeyException으로 전환하는 기능을 확인해보는 학습테스트

```java
@Test
public void sqlExceptionTranslate(){
	dao.deleteAll();
	try{
		dao.add(user1);
		dao.add(user1);
	}catch(DuplicateKeyException ex){
		SQLException sqlEx = (SQLException)ex.getRootCause();
		SQLExceptionTranslator set = //코드를 이용한 SQLException의 전환
			new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
		assertThat(set.thanslate(null,null,sqlEx)),
			is(DuplicateKeyException.class));
	}
}
```

강제로 DuplicateKeyException을 발생시킨다 . DuplicateKeyException은 중첩된 예외 이므로 JBDC API에서 처음 발생한 SQLException을 갖고 있다 .getRootCause을 이용하면 중첩되어있는 SQLException을 가져올수 있다.

이제 검증해 볼 사항은 스프링의 예외 전환 API를 직접 적용해서 DuplicateKeyException 이 만들어 지는가이다.

주입받은 dataSource를 이용해서 SQLErrorCodeSQLExceptionTranslator의 오브젝트를 만든다. 그리고 SQLException을 파라미터로 넣어 translate()메소드를 호출해주면 DataAccessException 타입의 예외로 변환해준다 . **변환된 DataAccessException타입의 예외가 정확히 DuplicateKeyException타입인지를 확인하면된다.**

