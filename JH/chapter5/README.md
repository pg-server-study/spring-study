# 5장 서비스 추상화

5장에서는 지금까지 만든 DAO에 트랜잭션을 적용해보면서 스프링이 어떻게 성격이

비슷한 여러 종류의 기술을 추상화하고 이를 일관된 방법으로 사용할 수 있도록 지원하는지를

살펴본다.

---

## 5.1 사용자 레벨 관리 기능 추가

지금까지 만든 UserDao는 단순 CRUD 만 하는 기능만 있고 어떠한

비즈니스 로직도 존재하지않는다.

여기에 간단한 비즈니스로직을 추가해보겠다. 구현해야할 비즈니스 로직은 다음과 같다.

- 사용자의 레벨은 BASIC, SILVER, GOLD 세가지 중 하나다.
- 사용자가 처음 가입하면 BASIC 레벨이 되며, 이후 활동에 따라서 한 단계씩 업그레이드 될 수 있다.
- 가입 후 50회 이상 로그인을 하면 BASIC에서 SILVER 레벨이 된다.
- SILVER 레벨이면서 30번 이상 추천을 받으면 GOLD 레벨이 된다.
- 사용자 레벨의 변경 작업은 일정한 주기를 가지고 일괄적으로 진행된다. 변경 작업 전에는 조건을 충족하더라도 레벨의 변경이 일어나지 않는다.

### 5.1.1 필드 추가

**Level 이넘**

User 클래스에 사용자 레벨을 저장할 필드를 추가하자.

추가로 DB User Table 에는 어떤 타입으로 넣을 것인지, 또 여기에 매핑되는 자바 User 클래스에는 어떤

타입으로 넣을 것인지 생각해보자.

DB에 varchar 타입으로 선언하고 문자를 넣는 방법도 있지만, 이렇게 일정한 종류의 정보를

문자열로 넣는 것은 별로 좋아보이지 않는다.

대신 각 레벨을 코드화해서 숫자로 넣는건 어떨까? 범위가 작은 숫자로 관리하면 DB용량도 많이 차지하지 않고

가벼워서 좋다.

그럼 자바의 User에 추가할 프로퍼티 타입도 숫자로 하면될까?

이건 좋지 않다. 의미 없는 숫자를 프로퍼티에 사용하면 타입이 안전하지 않아서 위험할 수 있기 때문이다.

아래와 같은 타입으로 레벨을 사용한다 해보자

```java
public class User {
	private static final int BASIC = 1;
	private static final int SLIVER = 2;
	private static final int GOLD = 3;
	
	int level;

	public void setLevel(int level) {
		this.level = level;
	}
}
```

BASIC , SILVE, GOLD처럼 의미 있는 상수도 정해놨으니 DB에 저장될 때는 getLevel() 이 돌려주는 숫자 값을 사용 하면 된다.

```java
if (user1.getLevel() == user.BASIC) {
	user1.setLevel(User.SILVER);
}
```

문제는 level 의 타입이 int이기 때문에 위처럼 다른 종류의 정보를 넣는 실수를 해도 

컴파일러가 체크해주지 못한다는 점이다. 

우연히 getSum() 메소드가 1, 2, 3과 같은 값을 돌려주면 기능은 문제없이 돌아가는 것처럼 보이지만 

사실은 레벨이 엉뚱하게 바뀌는 심각한 버그가 생성된다.

```java
user1.setLevel(other.getSum());

// 또 위험한
user1.setLevel(1000);
```

그래서 숫자 타입을 직접 사용하는 것보단 자바5 이상에서 제공하는 ENUM 을 이용하는게 안전하고 편리하다.

Enum 을 만들어보자

```java
package com.example.springtoby.toby.enums;

public enum Level {

    BASIC(1),
    SIlVER(2),
    GOLD(3);

    private final int value;

    Level(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1: return BASIC;
            case 2: return SIlVER;
            case 3: return GOLD;
            default: throw new AssertionError("Unknown value: " + value);
        }
    }

}
```

이렇게 만들어진 Level 이넘은 내부에는 DB에 저장할 int 타입의 값을 갖고 있지만, 겉으로는

Level 타입의 오브젝트이기 때문에 안전하게 사용할 수 있다. user1.setLevel(1000) 과 같은 코드는

컴파일러가 타입이 일치하지 않는다는 에러를 내면서 걸러줄 것이다.

**User 필드 추가**

이렇게 만든 Level 타입의 변수를 User 클래스에 추가하자.

사용자 레벨 관리 로직에서 언급된 로그인 횟수와 추천수도추가하자.

```java
package com.example.springtoby.toby;
//스프링에 필요한 클래스를 정의합니다.

import com.example.springtoby.toby.enums.Level;

public class User {

    String id;
    String name;
    String password;
    Level level;
    int login;
    int recommend;

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public int getLogin() {
        return login;
    }

    public void setLogin(int login) {
        this.login = login;
    }

    public int getRecommend() {
        return recommend;
    }

    public void setRecommend(int recommend) {
        this.recommend = recommend;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

```sql
-- 변경된 스키마
create table users
(
    id       varchar(10) primary key,
    name     varchar(20) not null,
    password varchar(10) not null,
    level tinyint not null,
    login number not null,
    recommend number not null
)
```

**UserDaoTest 변경**

이제 변경된 코드에 맞춰 UserDaoTest 코드들도 변경해주자

몇가지 추가된게 있으니 아래의 코드를 확인하여 추가하자.

```java
// 파라미터 추가
@SpringBootTest
public class UserDaoTest {
		//...

		User user1;
    User user2;
    User user3;

    @BeforeEach
    public void setUp() {
        this.user1 = new User("gyumee", "박성철", "springno1", Level.BASIC, 1, 0);
        this.user2 = new User("leegw700", "이길원", "springno2", Level.SIlVER, 55, 10);
        this.user3 = new User("bumjin", "박범진", "springno3", Level.GOLD, 100, 40);

    }

}
```

SetUp 추가 및 테스트 케이스 추가

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserDAOTest {

    @Autowired
    UserDao userDao;

    User user1;
    User user2;
    User user3;

    @BeforeEach
    public void setUp() {
        this.user1 = new User("gyumee", "박성철", "springno1", Level.BASIC, 1, 0);
        this.user2 = new User("leegw700", "이길원", "springno2", Level.SIlVER, 55, 10);
        this.user3 = new User("bumjin", "박범진", "springno3", Level.GOLD, 100, 40);

    }

    @Test
    public void addAndGet() {

        userDao.add(user1);
        userDao.add(user2);

        User userGet1 = userDao.get(user1.getId());
        checkSameUser(userGet1, user1);

        User userGet2 = userDao.get(user2.getId());
        checkSameUser(userGet2, user2);

    }

    private void checkSameUser(User user1, User user2) {
        assertThat(user1.getId()).isEqualTo(user2.getId());
        assertThat(user1.getName()).isEqualTo(user2.getName());
        assertThat(user1.getPassword()).isEqualTo(user2.getPassword());
        assertThat(user1.getLevel()).isEqualTo(user2.getLevel());
        assertThat(user1.getLogin()).isEqualTo(user2.getLogin());
        assertThat(user1.getRecommend()).isEqualTo(user2.getRecommend());
    }

}
```

**전체 테스트 코드**

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserDAOTest {

    @Autowired
    UserDao userDao;

    User user1;
    User user2;
    User user3;

    @BeforeEach
    public void setUp() {
        this.user1 = new User("gyumee", "박성철", "springno1", Level.BASIC, 1, 0);
        this.user2 = new User("leegw700", "이길원", "springno2", Level.SIlVER, 55, 10);
        this.user3 = new User("bumjin", "박범진", "springno3", Level.GOLD, 100, 40);

    }

    @Test
    @DisplayName("데이터베이스에 유저 등록 및 조회 테스트")
    void addUserTest() {

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

    @Test
    public void addAndGet() {

        userDao.add(user1);
        userDao.add(user2);

        User userGet1 = userDao.get(user1.getId());
        checkSameUser(userGet1, user1);

        User userGet2 = userDao.get(user2.getId());
        checkSameUser(userGet2, user2);

    }

    private void checkSameUser(User user1, User user2) {
        assertThat(user1.getId()).isEqualTo(user2.getId());
        assertThat(user1.getName()).isEqualTo(user2.getName());
        assertThat(user1.getPassword()).isEqualTo(user2.getPassword());
        assertThat(user1.getLevel()).isEqualTo(user2.getLevel());
        assertThat(user1.getLogin()).isEqualTo(user2.getLogin());
        assertThat(user1.getRecommend()).isEqualTo(user2.getRecommend());
    }

}
```

테스트를 돌려보면 실패할것이다.

**UserDaoJdbc 수정**

필드가 변경되었으니 그에 메소드를 수정해주자.

```java
@Override
public User mapRow(ResultSet rs, int rowNum) throws SQLException {
     User user = new User();
     user.setId(rs.getString("id"));
     user.setName(rs.getString("name"));
     user.setPassword(rs.getString("password"));
     user.setLevel(Level.valueOf(rs.getInt("level")));
     user.setLogin(rs.getInt("login"));
     user.setRecommend(rs.getInt("recommend"));
     return user;
}

public void add(final User user) throws DuplicateUserIdException {
    this.jdbcTemplate
            .update("insert into users(id, name, password) values(?,?,?,?,?,?)"
                    , user.getId(), user.getName(), user.getPassword(), user.getLevel().getValue(), user.getLogin(), user.getRecommend());
}
```

level, login, recommend 가 추가되었다.

여기서 눈여겨 볼것은  Level 타입의 level 필드를 사용하는 부분이다.

Level Enum은 오브젝트이므로 DB에 저장될 수 있는 SQL 타입이 아니다.

따라서 DB에 저장 가능한 정수형 값으로 변환해줘야 한다.

add() 메소드에서는 getValue() 메소드를 사용해서 가져온다.

반대로 조회할땐 Enum에 미리 만들어둔 valueOf() 를 이용해 int 타입의 Enum 오브젝트로 만들어서 넣어줘야한다.

만약에 여기에서 쿼리문에 login 이 아닌 loign 이라는 오타가 들어간다면 프로그램이 실행되면서

DB에 전달되기 전까지 문법 오류를 발견하기 힘들다는게 문제다. 

지금은 미리미리 DB까지 연동되는 테스트를 잘 만들어뒀기 때문에 SQL문장 오류도 금방 잡아낼 수 있다.

만약에 테스트를 작성하지 않았더라면 프로덕션에서 큰 문제가 일어났을 것이다

### 5.1.2 사용자 수정 기능 추가

사용자 관리 비즈니스 로직에 따르면 사용자 정보는 여러번 수정 될 수 있다.

상식적으로 id (pk) 값을 제외하고는 나머지 필드는 수정 될 수 있다.

현재 우리 UserDao 시스템은 단순하고 필드도 몇 개 되지 않으므로 간단히 접근해보자

**수정 기능 테스트 추가**

테스트 코드를 먼저 작성후 프로덕션 코드를 작성하자.

```java
@Test
public void update() throws SQLException {
    userDao.deleteAll();

    userDao.add(user1);

    user1.setName("오민규");
    user1.setPassword("springno6");
    user1.setLevel(Level.GOLD);
    user1.setLogin(1000);
    user1.setRecommend(999);
    userDao.update(user1);

    User user1Update = userDao.get(user1.getId());
    checkSameUser(user1, user1Update);
}
```

올바르게 update 프로덕션 코드를 작성했다면 위의 테스트 코드는 통과해야 한다

**UserDao와 UserDaoJdbc 수정**

이제 update 코드를 작성하고 테스트를 돌려보자.

```java
class UserDao {

	public void update(User user) {
        jdbcTemplate.update(
                "update users " +
                        "set name = ?, password = ?, level =?, login = ?, " +
                        "recommend = ? where id = ?",
                user.getName(), user.getPassword(),
                user.getLevel().getValue(), user.getLogin(),
                user.getRecommend(),
                user.getId()
        );
    }

}
```

![Untitled](5%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%89%E1%85%A5%E1%84%87%E1%85%B5%E1%84%89%E1%85%B3%20%E1%84%8E%E1%85%AE%E1%84%89%E1%85%A1%E1%86%BC%E1%84%92%E1%85%AA%20fa1ce9a7f9fc451c806e596a21fe6c37/Untitled.png)

```java
2021-11-07 17:22:02.120 DEBUG 3847 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyStatement.executeUpdate(ProxyStatement.java:119)
1. delete from users

2021-11-07 17:22:02.128 DEBUG 3847 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('gyumee','박성철','springno1',1,1,0)

2021-11-07 17:22:02.130 DEBUG 3847 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. update users set name = '오민규', password = 'springno6', level =3, login = 1000, recommend = 999 where id = 'gyumee'

2021-11-07 17:22:02.133 DEBUG 3847 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'gyumee'
```

정상적으로 테스트가 성공한다!

**수정 테스트 보완**

테스트가 성공했으니 이제 다른 작업을 할 수도 있다.

하지만 꼼꼼한 개발자라면 이 테스트에 불만을 가지고 의심스럽게 코드를 다시 살펴볼 것이다.

저렇게 문자열로 SQL 을 사용하는곳에서 가장 많이 일어나는 실수가 SQL문을 잘못 입력하는경우다

해당 SQL 문법에서 where 절을 빠져도 테스트 코드는 통과한다 모든 컬럼을 변경하기 떄문이다.

그렇다면 어떻게 해야할까?

첫 번째 방법은 update()가 돌려주는 리턴 값을 확인하는 것이다.

JdbcTemplate 는 테이블에 영향을 주는 SQL을 실행하면 영향받은 로우의 개수를 돌려준다.

그러면? update의 return 값이 1인지 확인하면되는것이다.

두 번째 방법은 사용자를 두명 등록하고 한명은 변경하지 않은 것을 확인하는 것이다.

두 번째 방법으로 테스트를 보완해보자.

```java
class UserDaoTest {
		@Test
    public void update() throws SQLException {
        userDao.deleteAll();

        userDao.add(user1);
        userDao.add(user2); // 수정 하지 않을 사용자

        user1.setName("오민규");
        user1.setPassword("springno6");
        user1.setLevel(Level.GOLD);
        user1.setLogin(1000);
        user1.setRecommend(999);
        userDao.update(user1);

        User user1Update = userDao.get(user1.getId());
        checkSameUser(user1, user1Update);
        
        User user2Update = userDao.get(user2.getId());
        checkSameUser(user2, user2Update);

    }
}
```

해당 테스트로 where를 빼먹으면 실패로 끝날 것이다.

### 5.1.3 UserService.upgradeLevels()

레벨 관리 기능을 구현하기는 어렵지 않다.

테스트를 거친 UserDao도 준비되어 있으니

데이터 액세스 기능은 문제 없다. UserDao의 getAll() 메소드로 사용자를 다 가져와서

사용자별로 레벨 업그레이드를 진행하면서 UserDao의 update() 를 호출해 DB에 결과를 넣어주면 된다.

비즈니스 로직은 어디에 둘까?

UserService라는 곳에 비즈니스 코드를 작성하자 UserService는 UserDao 빈을 DI 받아 사용한다.

아래는 스프링 부트 기준으로 작성된 UserService 다.

```java
package com.example.springtoby.toby;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserDao userDao;
    
    
    
}
```

UserService에 대한 테스트 클래스도 작성하자.

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;
    
    @DisplayName("빈이 올바르게 주입되었는지 테스트한다.")
    @Test
    public void bean() {
        assertThat(this.userService).isNotNull();
    }

}
```

불안하니 올바르게 빈 주입이 되었는지도 테스트한다. 테스트코드는 물론 통과한다.

**upgradeLevels() 메소드**

이제 로직을 추가해보자. 앞에 내용을보고 로직을 구현해보면 다음과 같다.

```java
package com.example.springtoby.toby;

import com.example.springtoby.toby.enums.Level;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserDao userDao;

    public void upgradeLevels() {
        List<User> userList = userDao.getAll();
        
        for(User user : userList) {
            Boolean changed = null;

            if (user.getLevel() == Level.BASIC && user.getRecommend() >= 50) {
                user.setLevel(Level.SIlVER);
                changed = true;
            } else if (user.getLevel() == Level.SIlVER && user.getRecommend() >= 30) {
                user.setLevel(Level.GOLD);
                changed = true;
            } else if (user.getLevel() == Level.GOLD) {
                changed = true;
            } else {
                changed = false;
            }

            if (changed) {
                userDao.update(user);
            }
            
        }
        
    }
}
```

User를 전부 가져와서 변경 조건이 맞으면 flag를 true로 변경하고 업데이트를 진행한다.

**upgradeLevels() 테스트**

테스트 방법을 생각해보자. 적어도 가능한 모든 조건을 하나씩은 확인 해야 한다.

UserService의 테스트 코드를 작성해보자.

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.UserService;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> userList;

    @BeforeEach
    public void setUp() {
        userList = List.of(
                new User("bumjin", "박범진", "p1", Level.BASIC, 49, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, 50, 0),
                new User("erwins", "신승한", "p3", Level.SIlVER, 60, 29),
                new User("madnite1", "이상호", "p4", Level.SIlVER, 60, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }

    @DisplayName("빈이 올바르게 주입되었는지 테스트한다.")
    @Test
    public void bean() {
        assertThat(this.userService).isNotNull();
    }

    @Test
    public void upgradeLevels() throws SQLException {

        userDao.deleteAll();

        for(User user : userList) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(userList.get(0), Level.BASIC);
        checkLevel(userList.get(1), Level.SIlVER);
        checkLevel(userList.get(2), Level.SIlVER);
        checkLevel(userList.get(3), Level.GOLD);
        checkLevel(userList.get(4), Level.GOLD);

    }

    private void checkLevel(User user, Level expectedLevel) {
        User userUpdate = userDao.get(user.getId());
        assertThat(userUpdate.getLevel()).isEqualTo(expectedLevel);
    }

}
```

테스트를 돌려보면

```java
2021-11-07 17:51:42.595 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyStatement.executeUpdate(ProxyStatement.java:119)
1. delete from users

2021-11-07 17:51:42.603 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('bumjin','박범진','p1',1,49,0)

2021-11-07 17:51:42.604 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('joytouch','강명성','p2',1,50,0)

2021-11-07 17:51:42.605 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('erwins','신승한','p3',2,60,29)

2021-11-07 17:51:42.605 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('madnite1','이상호','p4',2,60,30)

2021-11-07 17:51:42.606 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('green','오민규','p5',3,100,100)

2021-11-07 17:51:42.606 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyStatement.executeQuery(ProxyStatement.java:110)
1. select * from users order by id

2021-11-07 17:51:42.616 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. update users set name = '오민규', password = 'p5', level =3, login = 100, recommend = 100 where id = 'green'

2021-11-07 17:51:42.617 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. update users set name = '강명성', password = 'p2', level =2, login = 50, recommend = 0 where id = 'joytouch'

2021-11-07 17:51:42.617 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. update users set name = '이상호', password = 'p4', level =3, login = 60, recommend = 30 where id = 'madnite1'

2021-11-07 17:51:42.618 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'bumjin'

2021-11-07 17:51:42.648 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'joytouch'

2021-11-07 17:51:42.649 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'erwins'

2021-11-07 17:51:42.649 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'madnite1'

2021-11-07 17:51:42.649 DEBUG 4019 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'green'
```

테스트가 성공한다.

이제 레벨 변경기능은 만들어 졌다.

### 5.1.4 UserService.add()

사용자 관리 비즈니스 로직에서 대부분 구현했지만 아직 구현 못한 하나가 남았다.

처음 가입하는 사용자는 기본적으로 BASIC 레벨이어야 한다.

이 로직은 어디에 담을까?

UserDao는 관심사에 맞지 않는거같다

그러면 어디에 넣을까? UserService에 이 비즈니스 로직을 넣는건 어떨까?

결정이 되었으니 진행해보자.

```java
//UserService 에 Add 메소드 추가

public UserService {
	/// ...

		public void add(User user) {
        if(user.getLevel() == null) user.setLevel(Level.BASIC);
        userDao.add(user);
    }
}
```

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.UserService;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> userList;

    @BeforeEach
    public void setUp() {
        userList = List.of(
                new User("bumjin", "박범진", "p1", Level.BASIC, 49, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, 50, 0),
                new User("erwins", "신승한", "p3", Level.SIlVER, 60, 29),
                new User("madnite1", "이상호", "p4", Level.SIlVER, 60, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }

    @DisplayName("빈이 올바르게 주입되었는지 테스트한다.")
    @Test
    public void bean() {
        assertThat(this.userService).isNotNull();
    }

    @Test
    public void add() throws SQLException {
        userDao.deleteAll();

        User userWithLevel = userList.get(4); // GOLD LEVEL
        User userWithoutLevel = userList.get(0);
        userWithoutLevel.setLevel(null); // 비어있는 레벨 설정

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel()).isEqualTo(userWithLevel.getLevel());
        assertThat(userWithoutLevelRead.getLevel()).isEqualTo(Level.BASIC);

    }

}
```

UserService 테스트도 구현되었다 테스트를 실행해보자.

해당 코드도 통과한다!

```java
2021-11-07 18:00:10.632 DEBUG 4062 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyStatement.executeUpdate(ProxyStatement.java:119)
1. delete from users

2021-11-07 18:00:10.642 DEBUG 4062 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('green','오민규','p5',3,100,100)

2021-11-07 18:00:10.644 DEBUG 4062 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users(id, name, password, level, login, recommend) values('bumjin','박범진','p1',1,49,0)

2021-11-07 18:00:10.648 DEBUG 4062 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'green'

2021-11-07 18:00:10.654 DEBUG 4062 --- [           main] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'bumjin'
```

현재 로직은 레벨이 비어있으면 BASIC를 넣어주고 있다 INSERT 문도 올바르게 실행된다.

### 5.1.5 코드 개선

이제 비즈니스 로직의 구현을 모두 마쳤다.

깔끔한 코드를 추구하는 스프링 사용자답게 만들어진 코드를 다시 한번 검토해보자.

- 코드에 중복된 부분은 없는가?
- 코드가 무엇을 하는 것인지 이해하기 불편하지 않은가?
- 코드가 자신이 있어야 할 자리에 있는가?
- 앞으로 변경이 일어난다면 어떤 것이 있을 수 있고, 그 변환에 쉽게 대응할 수 있게 작성되어 있는가?

**upgradeLevels() 메소드 코드의 문제점**

위의 질문을 하며 서비스의 메소드를 보면 몇가지 문제점이있다.

if else 문이 많아 코드를 읽기 불편하다.

혹시라도 조건과 등급이 많아지며 더욱 불편해질것이다 

리팩토링을 진행해보자

**upgradeLevels() 리팩토링**

```java
package com.example.springtoby.toby;

import com.example.springtoby.toby.enums.Level;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserDao userDao;

    public void upgradeLevels() {
        List<User> userList = userDao.getAll();

        for(User user : userList) {
            
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }
    
    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC: return (user.getLogin() >= 50);
            case SILVER: return (user.getRecommend() >= 30);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unknown Level " + currentLevel);
        }
    }
    
    private void upgradeLevel(User user) {
        if (user.getLevel() ==Level.BASIC) user.setLevel(Level.SIlVER);
        else if (user.getLevel() == Level.SIlVER) user.setLevel(Level.GOLD);
        userDao.update(user);
    }

}
```

이제 단위별로 분리하여 코드가 읽기 쉬워졌다.

진행 플로우를 보자

모든 사용자를 가져와서 한명씩 업그레이드가 가능한지 체크하고

업그레이드가 가능하다면 true를 리턴해 업그레이드 시켜준다.

GOLD는 그 위의 등급이 없기 때문에 무조건 false 를 주고 있다.

전보다 코드가 매우 깔끔해졌다.

upgradeLevel() 메소드는

레벨 업그레이드를 위한 작업은 사용자의 레벨을 다음 단계로 바꿔주는 변경사항을 DB에 업데이트 해주는 것이다.

이제 테스트를 돌려보자 테스트가 통과하면 로직에 문제는 없는 것이다.

그런데 upgradeLevel() 메소드 코드는 마음에 안든다 다음단계가 무엇인지를

메소드에서 정해주지않고 enum에게 넘기자.

enum을 수정해보자.

```java
package com.example.springtoby.toby.enums;

public enum Level {

    GOLD(3, null),
    SIlVER(2, GOLD),
    BASIC(1, SIlVER);

    private final int value;
    private final Level next;

    Level(int value, Level next) {
        this.value = value;
        this.next = next;
    }

    public int getValue() {
        return value;
    }

    public Level getNext() {
        return next;
    }

    public static Level valueOf(int value) {
        switch (value) {
            case 1: return BASIC;
            case 2: return SIlVER;
            case 3: return GOLD;
            default: throw new AssertionError("Unknown value: " + value);
        }
    }

}
```

Enum에 Level 타입의 next를 추가하고

User 클래스에 업데이트하는 메소드를 추가해보자.

```java
class User {
	public void upgradeLevel() {
        Level nextLevel = this.level.getNext();
        if (nextLevel == null) {
            throw new IllegalArgumentException(this.level + "은 업그레이드가 불가능합니다.");
        } else {
            this.level = nextLevel;
        }
    }
}
```

이제 Service 만 변경해주면된다.

```java
class UserService {
	//... 

	private void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
  }
}
```

정말 알아보기 편하고 깔끔해지고 책임도 분리가 되었다.

이제 책임을 user 와 enum에게 넘겼다.

이게 완벽한 코드는 아니지만 많은 발전을 이루었다.

항상 코드를 더 깔끔하고 유연하면서 변화에 대응하기 쉽고 테스트하기 좋게 만들려고 노력해야함을 기억하며

다음으로 넘어가자.

**User 테스트**

방금 User에 간단하지만 로직을 담은 메소드를 추가했다.

이것도 테스트하는게 좋을까? 이 정도는 테스트 없이 넘어갈 수도 있겠지만

앞으로 계속 새로운 기능과 로직이 추가될 가능성이 있으니 테스트를 만들어 두면 도움이 될 것이다

upgradeLevel() 메소드에 대한 테스트다

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

    User user;

    @BeforeEach
    public void setUp() {
        this.user = new User();
    }

    @Test
    public void upgradeLevel() {
        Level[] levels = Level.values();

        for(Level level : levels) {
            if (level.getNext() == null) continue;
            user.setLevel(level);
            user.upgradeLevel();
            assertThat(user.getLevel()).isEqualTo(level.getNext());
        }
    }

}
```

User의 권한을 업그레이드하고 비교하는 테스트다

당연히 성공한다 이제 업그레이드에 대한 테스트 코드도 작성이 완료되었다.

**UserServiceTest 개선**

UserServiceTest 테스트도 혹시 손볼 데가 없을지 살펴보자.

checkLevel() 메소드를 호추할 때 일일이 다음 단계의 레벨이 무엇인지 넣어줬다.

이것도 사실 중복이다. Level이 갖고 있어야 할 다음 레벨이 무엇인가 하는 정보를 테스트에 직접 넣어둘 필요가 없다.

아래와 같이 만드는게 간결하다

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.UserService;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> userList;

    @BeforeEach
    public void setUp() {
        userList = List.of(
                new User("bumjin", "박범진", "p1", Level.BASIC, 49, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, 50, 0),
                new User("erwins", "신승한", "p3", Level.SIlVER, 60, 29),
                new User("madnite1", "이상호", "p4", Level.SIlVER, 60, 30),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }

    @Test
    public void upgradeLevels() throws SQLException {

        userDao.deleteAll();

        for(User user : userList) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(userList.get(0), false);
        checkLevel(userList.get(1), true);
        checkLevel(userList.get(2), false);
        checkLevel(userList.get(3), true);
        checkLevel(userList.get(4), false);

    }

    private void checkLevel(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());

        if (upgraded) {
            assertThat(userUpdate.getLevel()).isEqualTo(user.getLevel().getNext());
        } else {
            assertThat(userUpdate.getLevel()).isEqualTo(user.getLevel());
        }

    }

}
```

true false 를 주고 내부값으로 비교하게 만들었다.

이제 UserService 를 보자 변경될수도 있는 값들은 하드코딩 하는것보단 상수화 시켜서 하나만 변경해도

다른것들도 변경되게 만드는게 좋다.

```java
class UserService {
	public static final int MIN_LOG_COUNT_FOR_SILVER = 50;
  public static final int MIN_RECOMMEND_FOR_GOLD = 30;
	
	private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();
        switch (currentLevel) {
            case BASIC: return (user.getLogin() >= MIN_LOG_COUNT_FOR_SILVER);
            case SIlVER: return (user.getRecommend() >= MIN_RECOMMEND_FOR_GOLD);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unknown Level " + currentLevel);
        }
    }

}
```

위의 상수값을 다른곳에서도 사용하게 하자.

```java
import static com.example.springtoby.toby.UserService.MIN_LOG_COUNT_FOR_SILVER;
import static com.example.springtoby.toby.UserService.MIN_RECOMMEND_FOR_GOLD;

class UserServiceTest {
	
		@BeforeEach
    public void setUp() {
        userList = List.of(
                new User("bumjin", "박범진", "p1", Level.BASIC, MIN_LOG_COUNT_FOR_SILVER -1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOG_COUNT_FOR_SILVER, 0),
                new User("erwins", "신승한", "p3", Level.SIlVER, 60, MIN_RECOMMEND_FOR_GOLD -1),
                new User("madnite1", "이상호", "p4", Level.SIlVER, 60, MIN_RECOMMEND_FOR_GOLD),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }
	
}
```

이러면 테스트 코드는 서비스의 바껴도 같이 수정해줄 번거로움이 사라진다.

---

## 5.2 트랜잭션 서비스 추상화

이제 사용자에 레베 관리 기능에 대한 구현을 마쳤고 테스트를 통해 검증도 끝났다.

이쯤에서 마무리 해도될거같은데 이런 상황이 발생하면 어떻게 될까?

**"정기 사용자 레벨 관리 작업을 수행중 네트워크가 끊켜서 서버에 장애로 인하여 작업을 완료할 수 없다면 그동안 변경된 작업을 그대로 둬야하나요 초기로 둬야하나요? "**

### 5.2.1 모 아니면 도

그럼 테스트를 만들어 확인해보자. 그런데 이번테스트는 간단하지 않다.

예외적인 상황을 작업 중간에 강제로 발생시켜야 하기 때문이다.

현재로서 방법은 예외가 던져지는 상황을 의도적으로 만드는게 나을 것이다.

이번에는 테스트용으로 만든 UserService의 대역을 사용하는 방법이 좋다.

UserService를 상속해서 일부 메소드를 오버라이딩 하는 방법이 나을 것 같다.

현재 5개의 테스트용 사용자 정보중 두번째와 네번째가 업그레이드 대상이다

그러면 네번째 사용자를 처리하는중 예외를 발생시키자.

현재 UserService 는 private 지만 이번만 protected로 변경하여 오버라이딩 하자.

스프링 부트로 구현을 하고있기에 토비의 스프링과는 조금 코드가 다릅니다.

```java
class UserServiceTest {

	static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id, UserDao userDao) {
            super(userDao);
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {

    }
}
```

**강제 외예 발생을 통한 테스트**

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.UserService;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static com.example.springtoby.toby.UserService.MIN_LOG_COUNT_FOR_SILVER;
import static com.example.springtoby.toby.UserService.MIN_RECOMMEND_FOR_GOLD;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> userList;

    @BeforeEach
    public void setUp() {
        userList = List.of(
                new User("bumjin", "박범진", "p1", Level.BASIC, MIN_LOG_COUNT_FOR_SILVER - 1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOG_COUNT_FOR_SILVER, 0),
                new User("erwins", "신승한", "p3", Level.SIlVER, 60, MIN_RECOMMEND_FOR_GOLD - 1),
                new User("madnite1", "이상호", "p4", Level.SIlVER, 60, MIN_RECOMMEND_FOR_GOLD),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }

    @DisplayName("빈이 올바르게 주입되었는지 테스트한다.")
    @Test
    public void bean() {
        assertThat(this.userService).isNotNull();
    }

    @Test
    public void upgradeLevels() throws SQLException {

        userDao.deleteAll();

        for (User user : userList) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(userList.get(0), false);
        checkLevel(userList.get(1), true);
        checkLevel(userList.get(2), false);
        checkLevel(userList.get(3), true);
        checkLevel(userList.get(4), false);

    }

    @Test
    public void add() throws SQLException {
        userDao.deleteAll();

        User userWithLevel = userList.get(4); // GOLD LEVEL
        User userWithoutLevel = userList.get(0);
        userWithoutLevel.setLevel(null); // 비어있는 레벨 설정

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel()).isEqualTo(userWithLevel.getLevel());
        assertThat(userWithoutLevelRead.getLevel()).isEqualTo(Level.BASIC);

    }

    @Test
    public void upgradeAllOrNothing() throws SQLException {
        UserService testUserService = new TestUserService(userList.get(3).getId(), userDao);

        userDao.deleteAll();

        for (User user : userList) userDao.add(user);

        try {
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkLevel(userList.get(1), false);

    }

    private void checkLevel(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());

        if (upgraded) {
            assertThat(userUpdate.getLevel()).isEqualTo(user.getLevel().getNext());
        } else {
            assertThat(userUpdate.getLevel()).isEqualTo(user.getLevel());
        }

    }

    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id, UserDao userDao) {
            super(userDao);
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {

    }

}
```

여기서는 전체 코드의 확인을 위해 모든 코드를 올린다.

upgradeAllOrNothing 를 보자 우리가 원하는건

3번에서 실패시 1번까지 업그레이드가 실패로 돌아가는거다 

upgradeLevels 테스트에서는 1번 사용자가 true 일경우 통과한다

이 코드를 보자  **checkLevel(userList.get(1), false);** 

**값을 주어 만약 실패해서 모든게 돌아가면 이 테스트코드는 성공해야 한다.**

```java
org.opentest4j.AssertionFailedError: 
expected: BASIC
but was : SIlVER
Expected :BASIC
Actual   :SIlVER
```

하지만 실패한다 왜일까?

바로 트랜잭션의 문제다

현재 동작하는 업데이트가 하나의 트랜잭션으로 동작하지 않기 때문이다

트랜잭션이란 더 이상 나눌 수 없는 단위 작업을 말한다.

따라서 중간에 예외가 발생할 경우 모두 초기 상태로 돌려야한다.

### 5.2.2 트랜잭션 경계설정

트랜잭션에 대한 예시를 하나 들면

은행 이체를 예를 들 수 있다.

만약 이체 할때 트랜잭션의 묶음으로 안된다면

돈이 이체 될때 에러가 발생하고 해당 계좌에서는 돈이 빠져나갔지만 입금이 안된 상황이 발생할 수 있다.

이 때 트랜잭션을 적용 한다면 여기서 작업이 취소되고 초기 상태로 돌아가는 작업을

트랜잭션 롤백이라고 한다.

이제 트랜잭션을 적용해보자

UserService 에 트랜잭션 매니저를 주입시켜주자

```java
public class UserService {

		private final UserDao userDao;
    private final PlatformTransactionManager transactionManager;

    public static final int MIN_LOG_COUNT_FOR_SILVER = 50;
    public static final int MIN_RECOMMEND_FOR_GOLD = 30;

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

```java
package com.example.springtoby.domain;

import com.example.springtoby.toby.User;
import com.example.springtoby.toby.UserDao;
import com.example.springtoby.toby.UserService;
import com.example.springtoby.toby.enums.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static com.example.springtoby.toby.UserService.MIN_LOG_COUNT_FOR_SILVER;
import static com.example.springtoby.toby.UserService.MIN_RECOMMEND_FOR_GOLD;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    @Autowired
    PlatformTransactionManager transactionManager;

    List<User> userList;

    @BeforeEach
    public void setUp() {
        userList = List.of(
                new User("bumjin", "박범진", "p1", Level.BASIC, MIN_LOG_COUNT_FOR_SILVER - 1, 0),
                new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOG_COUNT_FOR_SILVER, 0),
                new User("erwins", "신승한", "p3", Level.SIlVER, 60, MIN_RECOMMEND_FOR_GOLD - 1),
                new User("madnite1", "이상호", "p4", Level.SIlVER, 60, MIN_RECOMMEND_FOR_GOLD),
                new User("green", "오민규", "p5", Level.GOLD, 100, 100)
        );
    }

    @Test
    public void upgradeLevels() throws SQLException {

        userDao.deleteAll();

        for (User user : userList) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevel(userList.get(0), false);
        checkLevel(userList.get(1), true);
        checkLevel(userList.get(2), false);
        checkLevel(userList.get(3), true);
        checkLevel(userList.get(4), false);

    }

    @Test
    public void upgradeAllOrNothing() throws SQLException {
        UserService testUserService = new TestUserService(userList.get(3).getId(), userDao, transactionManager);

        userDao.deleteAll();

        for (User user : userList) userDao.add(user);

        try {
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkLevel(userList.get(1), false);

    }

    private void checkLevel(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());

        if (upgraded) {
            assertThat(userUpdate.getLevel()).isEqualTo(user.getLevel().getNext());
        } else {
            assertThat(userUpdate.getLevel()).isEqualTo(user.getLevel());
        }

    }

    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id, UserDao userDao, PlatformTransactionManager transactionManager) {
            super(userDao, transactionManager);
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) throw new TestUserServiceException();
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {

    }

}
```

트랜잭션 매니저를 넣고 이제 테스트를 돌려보자..!

![Untitled](5%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%89%E1%85%A5%E1%84%87%E1%85%B5%E1%84%89%E1%85%B3%20%E1%84%8E%E1%85%AE%E1%84%89%E1%85%A1%E1%86%BC%E1%84%92%E1%85%AA%20fa1ce9a7f9fc451c806e596a21fe6c37/Untitled%201.png)

---

## 5.5 정리

- 비즈니스 로직을 담은 코드는 데이터 액세스 로직을 담은 코드와 깔끔하게 분리되는 것이 바람직하다. 비즈니스 로직 코드 또한 내부적으로 책임과 역할에 따라서 깔끔하게 메소드로 정리돼야 한다.
- 이를 위해서는 DAO의 기술 변화에 서비스 계층의 코드가 영향을 받지 않도록 인터페이스와 DI를 잘 활용해서 결합도를 낮춰줘야 한다.
- DAO를 사용하는 비즈니스 로직에는 단위 작업을 보장해주는 트랜잭션이 필요하다.
- 트랜잭션의 시작과 종료를 지정하는 일을 트랜잭션 경계설정이라고 한다. 트랜잭션 경계설정은 주로 비즈니스 로직 안에서 일어나는 경우가 많다.
- 시작된 트랜잭션 정보를 담은 오브젝트를 파라미터로 DAO에 전달하는 방법을 매우 비효율적이기 때문에 스프링이 제공하는 트랜잭션 동기화 기법을 활용하는 것이 편리하다.