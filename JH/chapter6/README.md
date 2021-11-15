# 6장 AOP

AOP 는 IoC , DI, 서비스 추상화와 더불어 스프링 3대 기반기술의 하나이다.

AOP는 정말 중요한 개념이다 해당 장을 이용하여 자세히 알아보도록 하자

---

## 6.1 트랜잭션 코드의 분리

지금까지 서비스 추상화 기법을 적용해 트랜잭션 기술에 독립적으로 만들어줬고

다른 코드들도 깔끔하게 만들었다.

하지만 찜찜하다 더 깔끔한 코드를 만들고 싶다 비즈니스 로직에서 트랜잭션 로직을 분리해보자

### 6.1.1 메소드 분리

기존 코드를 보면 뚜렷하게 두가지 종류의 코드가 구분되어 있다.

트랜잭션과 비즈니스가 존재한다

비즈니스 로직을 담당하는 코드를 메소드로 추출해서 독립시켜 보자.

**분리 전**

```java
class UserService {

	public void upgradeLevels() {

        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            List<User> userList = userDao.getAll();

            for(User user : userList) {

                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }

}
```

**분리 후**

```java
class UserService {

	public void upgradeLevels() {

        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            
            upgradeLevelsInternal();
            
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }

    private void upgradeLevelsInternal() {
        List<User> userList = userDao.getAll();

        for(User user : userList) {

            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }

}
```

코드가 보기 편해졌다. 테스트 코드를 다시 돌려보는것도 있지 말자

### 6.1.2 DI를 이용한 클래스의 분리

비즈니스 로직이 분리돼서 보기 좋지만

트랜잭션을 담당하는 기술적인 코드가 UserService 안에 자리 잡고 있다.

정보를 주고받는것도 아닌데 UserService에서는 보이지 않게 할수 있지 않을까?

트랜잭션 코드를 클래스 밖으로 뽑아내자.

### **DI 적용을 이용한 트랜잭션 분리**

UserService는 UsertServiceTest가 클라이언트로 사용중이다.

실전에서는 다른 클래스가 호출할 것이다.

그런데 UserService는 현재 클래스로 되어 있으니

다른 코드에서 사용한다면 UserService 클래스를 직접 참조하게 된다.

그렇다면 트랜잭션 코드를 UserService 밖으로 빼버리면 트랜잭션 기능이 빠진

UserService 가 되어버린다.

직접 사용하는 것이 문제가 된다면 간접적으로 사용하면 된다

DI의 기본 아이디어는 실제 사용할 오브젝트의 클래스 정체는 감춘 채

인터페이스를 통해 간접적으로 접근하는 것이다.

그 덕분에 구현 클래스는 얼마든지 외부에서 변경할 수 있다.

현재는 아래 그림처럼 클라이언트간의 관계가 강한 결합도로 고정되어 있다.

![Untitled](6%E1%84%8C%E1%85%A1%E1%86%BC%20AOP%20ae33e10418e14897b76240b053f52725/Untitled.png)

그래서 다음과 같이 UserService를 인터페이스로 만들고 기존 코드는

인터페이스의 구현 클래스를 만들어넣도록 한다.

그럼 결합이 약해지고 직접 구현 클래스에 의존하지 않기 때문에 유연한 확장이 가능하다.

![Untitled](6%E1%84%8C%E1%85%A1%E1%86%BC%20AOP%20ae33e10418e14897b76240b053f52725/Untitled%201.png)

런타임 시에 DI를 통해 적용하는 방법을 쓰는 이유는, 일반적으로

구현클래스를 바꿔가면서 사용하기 위해서다.

정식 운영중에는 정규 구현 클래스를 DI 해주는 방법처럼 한 번에 한 가지 클래스를 선택해서

적용하도록 되어있다.

하지만 꼭 그래야 한다는 제약은 없다.

한 번에 두 개의 UserService 인터페이스 구현 클래스를 동시에 이용한다면 어떨까?

지금 문제는 순수하게 비즈니스 로직을 담고 있는 코드만 놔두고

트랜잭션 경계설정을 담당하는 코드를 외부로 빼내려는거다.

하지만 클라이언트가 UserService의 기능을 제대로 이용하려면 트랜잭션이 적용돼야 한다.

아래와 같은 구조를 생각해볼 수 있다.

UserService를 구현한 또다른 구현 클래스를 만든다.

이클래스는 단지 트랜잭션의 경계설정 이라는 책임을 맡고 있다.

스스로 비즈니스 로직은 담고 있지 않다. 그래서 또 다른 비즈니스 로직을 담고있는 UserService의

구현 클래스에 실제적인 로직 처리 작업을 위임하는 것이다.

![Untitled](6%E1%84%8C%E1%85%A1%E1%86%BC%20AOP%20ae33e10418e14897b76240b053f52725/Untitled%202.png)

### UserService 인터페이스 도입

UserService 를 UserServiceImpl 로 변경하고 트랜잭션 로직을 제거한후

UserService Interface를 만들어서 상속 받게 만들자.

```java
public interface UserService {

    void add(User user);
    void upgradeLevels();

}
```

```java
public class UserServiceImpl implements UserService {
//...
	public void upgradeLevels() {

        List<User> userList = userDao.getAll();

        for(User user : userList) {

            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }

}
```

### 분리된 트랜잭션 기능

이제 비즈니스 트랜잭션 처리를 담은 UserServiceTx를 만들어보자 UserServiceTx는 기본적으로

UserService를 구현하게 만든다. 그리고 같은 인터페이스를 구현한 다른 오브젝트에게

고스란히 작업을 위임하게 만들면 된다.

그러면 적어도 비즈니스 로직에 대해 UserServiceTx가 아무런 관여도 하지 않는다.

```java
public class UserServiceTx implements UserService {

    UserService userService;
    PlatformTransactionManager transactionManager;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void add(User user) {
        userService.add(user);
    }

    @Override
    public void upgradeLevels() {

        TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            userService.upgradeLevels();
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }

    }
}
```

자이제 TX에서는 트랜잭션 로직만 담당한다.

아래와 같은 구조가 된거다

![Untitled](6%E1%84%8C%E1%85%A1%E1%86%BC%20AOP%20ae33e10418e14897b76240b053f52725/Untitled%203.png)

### 트랜잭션 적용을 위한 DI 설정

이제 남은 것은 설정파일을 수정하는 부분이다.

-중요-

토비의 스프링에서는 XML 설정으로 나와있지만

저는 SpringBoot를 이용한 어노테이션 설정을 작성하였습니다.

**비즈니스 로직을 담고 있는 UserServiceImpl**

```java
@RequiredArgsConstructor
@Service("UserServiceImpl") // <- Bean 이름 설정
public class UserServiceImpl implements UserService {
	
..// 로직
}
```

**트랜잭션 로직을 담고 있는 UserServiceTx**

```java
@Service // <- 이름을 따로 주지 않을 경우 클래스명으로 등록됨.
public class UserServiceTx implements UserService {
	....//
}
```

**UserServiceTest 에서의 DI 방법**

```java
@SpringBootTest
public class UserServiceTest {

    @Qualifier("userServiceTx") // UserService 인터페이스 타입의 구현체 이름 DI
    @Autowired
    UserService userService;

}
```

---

## 6.2 고립된 단위 테스트

### 6.2.1 복잡한 의존관계 속의 테스트

UserService의 경우를 생각해보자.

UserService 는 현재 매우 간단한 기능만을 가지고 있지만 아래와 같이 의존되어있는게 많다.

![Untitled](6%E1%84%8C%E1%85%A1%E1%86%BC%20AOP%20ae33e10418e14897b76240b053f52725/Untitled%204.png)

UserService 에서 하나의 메소드만 테스트 하려해도

트랜잭션, 메일 , DB연결 등 많은 준비와 내부적으로 여러 환경이 같이돌아간다.

그러다 보면 오류를 찾으려해도 UserService에서 찾는게 아닌 다른 곳 에서 찾아야 할수도 있다.

그리고 막상 UserService는 가져온 목록을 가지고 간단한 계산을 하는 게 전부라면

배보다 배꼽이 더 큰 작업이 될지도 모른다.

### 6.2.2 테스트 대상 오브젝트 고립시키기

### 테스트를 위한 UserServiceImpl 고립

UserService를 고립시키면 아래와 같은 구성을 할 수 있다.

![Untitled](6%E1%84%8C%E1%85%A1%E1%86%BC%20AOP%20ae33e10418e14897b76240b053f52725/Untitled%205.png)

MockUserDao를 내부클래스로 선언해주자

```java
public class UserServiceTest {
	.../

		// Mock
    static class MockUserDao implements UserDao {

        private List<User> users;
        private List<User> updated = new ArrayList<>();

        private MockUserDao(List<User> users) {
            this.users = users;
        }

        public List<User> getUpdated() {
            return updated;
        }

        @Override
        public List<User> getAll() {
            return this.users;
        }

        @Override
        public void update(User user) {
            updated.add(user);
        }

        //테스트에 사용안함
        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();

        }

        @Override
        public void add(User user) throws DuplicateUserIdException {
            throw new UnsupportedOperationException();
        }

        @Override
        public User get(String id) {
            throw new UnsupportedOperationException();

        }
    }
}
```

현재 지금 upgradeLvels() 테스트만 할거니 나머지 사용하지 않은 메소드에 대해선

UnsupportedOperationException 를 던지게해서 지원하지 않는 기능이라고 명시해주자.

그리고 upgradeLevels 테스트 메소드를 변경해주자

```java
public class UserServiceTest {

		@Test
    public void upgradeLevels() throws SQLException {

        MockUserDao mockUserDao = new MockUserDao(this.userList);

        UserServiceImpl userService = new UserServiceImpl(mockUserDao); // 고립 테스트 직접생성

        userDaoImpl.deleteAll();

        userService.upgradeLevels();

        List<User> updated = mockUserDao.getUpdated();

        assertThat(updated.size()).isEqualTo(2);
        checkUserAndLevel(updated.get(0), "joytouch", Level.SIlVER);
        checkUserAndLevel(updated.get(1), "madnite1", Level.GOLD);

    }
    // upgradeLevels 에서 사용할 테스트용 메소드
    private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
        assertThat(updated.getId()).isEqualTo(expectedId);
        assertThat(updated.getLevel()).isEqualTo(expectedLevel);
    }

}
```

고립 테스트를위해 테스트의 대상을 직접 생성한다.

위에서 만들어진 UserList 를 Mock 객체에 넣고 로직을 수행한다.

테스트를 돌려보자 통과가 나올 것이다.

### 6.2.3 단위 테스트와 통합 테스트

단위 테스트의 단위는 정하기 나름이다.

중요한것은 단위에 초점을 맞춘 테스트다

**"테스트 대상 클래스를 목 오브젝트 등의 테스트 대역을 이용해 의존 오브젝트나 외부의 리소스를 사용하지 않도록 고립시켜서 테스트하는 것"** 을 단위 테스트라고 부른다

외부의 DB나 파일, 서비스 등의 리소스가 참여하는 테스트는 **통합 테스트** 라고 부른다.

통합테스트란 두개의 단위가 결합해서 동작하면서 수행되는 것이다.

### 가이드 라인

- 항상 단위 테스트를 먼저 고려한다.
- 하나의 클래스나 성격과 목적이 같은 긴밀한 클래스 몇 개를 모아서 외부와의 의존관계를 모두 차단하고 필요에 따라 스텁이나 목 오브젝트 등의 테스트 대역을 이용하도록 테스트를 만든다. 단위 테스트는 테스트 작성도 간단하고 실행 속도도 빠르며 테스트 대상 외의 코드나 환경으로부터 테스트 결과에 영향을 받지도 않기 때문에 가장 빠른 시간에 효과적인 테스트를 작성하기에 유리하다.
- 외부 리소스를 사용해야만 가능한 테스트는 통합 테스트로 만든다.
- 단위 테스트로 만들기가 어려운 코드도 있다. 대표적인 게 DAO다. DAO는 그 자체로 로직을 담고 있기 보다는 DB를 통해 로직을 수행하는 인터페이스와 같은 역할을 한다. SQL을 JDBC를 통해 실행하는 코드만으로는 고립된 테스트를 작성하기가 힘들다. 작성한다고 해도 가치가 없는 경우가 대부분이다. 따라서 DAO는 DB까지 연동하는 테스트로 만드는 편이 효과적이다. DB를 사용하는 테스트는 DB에 테스트 데이터를 준비하고, DB에 직접 확인을 하는 드으이 부가적인 작업이 필요하다
- DAO 테스트는 DB라는 외부 리소스를 사용하기 때문에 통합 테스트로 분류된다. 하지만 코드에서 보자면 하나의 기능 단위를 테스트하는 것이기도 하다. DAO를 테스트를 통해 층분히 검증해두면, DAO를 이용하는 코드는 DAO 역할을 스텁이나 목 오브젝트로 대체해서 테스트할 수 있다.이후 실제 DAO와 연동했을 때도 바르게 동작하리라고 확신할 수 있다. 물론 각각의 단위 테스트가 성공했더라도 여러개의 단위를 연걸해서 테스트하면 오류가 발생할 수도 있다. 하지만 충분한 단위 테스트를 거친다면 통합 테스트에서 오류가 발생할 확률도 줄어들고 발생한다고 하더라도 쉽게 처리할 수 있다
- 여러 개의 단위가 의존관계를 가지고 동작할 때를 위한 통합 테스트는 필요하다. 다만, 단위 테스트를 충분히 거쳤다면 통합 테스트의 부담은 상대적으로 줄어든다.
- 단위 테스트를 만들기가 너무 복잡하다고 판단되는 코드는 처음부터 통합 테스트를 고려해본다. 이때도 통합 테스트에 참여하는 코드 중에서 가능한 한 많은 부분을 미리 단위 테스트로 검증해두는 게 유리하다.
- 스프링 테스트 컨텍스트 프레임워크를 이용하는 테스트는 통합 테스트다. 가능하면 스프링의 지원 없이 직접 코드 레벨의 DI를 사용하면서 단위 테스트를 하는 게 좋겠지만 스프링의 설정 자체도 테스트 대상이고, 스프링을 이용해 좀 더 추상적인 레벨에서 테스트해야 할 경우도 종종 있다. 이럴 땐 스프링 테스트 컨텍스트 프레임워크를 이용해 통합 테스트를 작성한다.

### 6.2.4 목 프레임워크

매번 Mock 를 직접 만들기는 힘들다 다양한 목 오브젝트를 지원해주는 프레임워크를 알아보자

### Mockito 프레임워크

Mockito 같은 프레임워크를 사용하면 간단하게 Mock 객체를 만들 수 있다

변경된 테스트 코드

```java
public class UserServiceTest {
	
		@Test
    public void upgradeLevels() throws SQLException {

        UserDao mockUserDao = mock(UserDao.class);

        when(mockUserDao.getAll()).thenReturn(this.userList); // getAll() 할때 userList 리턴하도록 설정

        UserServiceImpl userService = new UserServiceImpl(mockUserDao); // Service 에 Mock 객체 주입

        userDaoImpl.deleteAll();

        userService.upgradeLevels();

        verify(mockUserDao, times(2)).update(any(User.class)); // 2번 호출되었는지 확인
        verify(mockUserDao, times(2)).update(any(User.class));

        verify(mockUserDao).update(userList.get(1)); // 
        assertThat(userList.get(1).getLevel()).isEqualTo(Level.SIlVER);

        verify(mockUserDao).update(userList.get(3));
        assertThat(userList.get(3).getLevel()).isEqualTo(Level.GOLD);

    }
	
}
```

MockIto 목 오브젝트의 단계별 사용법 두번째와 네번째는 각각 필요한 경우에만 사용할 수 있다

1. 인터페이스를 이용해 목 오브젝트를 만든다.
2. 목 오브젝트가 리턴할 값이 있으면 이를 지정해준다. 메소드가 호출되면 예외를 강제로 던지게 만들 수도 있다.
3. 테스트 대상 오브젝트에 DI해서 목 오브젝트가 테스트 중에 사용되도록 만든다.
4. 테스트 대상 오브젝트를 사용한 후에 목 오브젝트의 특정 메소드가 호출됐는지, 어떤 값을 가지고 몇 번 호출됐는지를 검증한다.

---

## 6.3 다이내믹 프록시와 팩토리 빈