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



