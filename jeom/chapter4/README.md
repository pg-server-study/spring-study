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

