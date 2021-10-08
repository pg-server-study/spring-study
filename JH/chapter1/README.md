# 1장 오브젝트와 의존관계

---

## ✏️ 목표

1장의 목표 스프링이 관심을 갖는 대상인 오브젝트(객체) 의 설계와 구현 동작원리에 대해 알게되고

스프링이 무엇인지에 대하여 학습

---

## 1.1 초난감 DAO

JDBC를 이용하여 DB에 저장하고 조회할 수 있는 DAO와 그 정보를 저장할 객체를 만들고

그 객체들로 앞으로의 내용에서 설명을 시작합니다

### 1.1.1 User ~ 1.1.2 UserDAO

**앞으로의 예제에서 사용될 Object**

```java
// DataBase 에서 가져온 정보들을 저장할User 클래스

public class User {

    String id;
    String name;
    String password;

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
// DataBase

create table users (
                       id varchar(10) primary key,
                       name varchar(20) not null,
                       password varchar(10) not null
)

```

```java

// 사용자 정보를 DB에 넣고 관리하는 DAO

// 해당 DAO 는 토비의 스프링에 있는 DAO 를 조금 커스텀 하였습니다.

public class UserDao {

    // Insert
    public void add(User user) throws ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");

        String sql = "insert into users(id, name, password) values(?,?,?,)";

        try(Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1,user.getId());
            ps.setString(2,user.getName());
            ps.setString(3,user.getPassword());

            int result = ps.executeUpdate();

            if(result != 1) {
                throw new SQLException();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    public User get(String id) throws ClassNotFoundException {

        Class.forName("com.mysql.jdbc.Drive");

        User user = new User();

        String sql = "select * from users where id = ?";

        try(Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));

            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return user;
    }

}

```

---

**스프링을 사용한 DAO 클래스**

> 해당 클래스는 spring 을 이용한 토비의 스프링에는 없는 코드입니다
>

```java
@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private DataSource dataSource;

    // Insert
    public void add(User user) throws ClassNotFoundException {
        String sql = "insert into users (id, name, password) values(?,?,?)";

        try(Connection connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1,user.getId());
            ps.setString(2,user.getName());
            ps.setString(3,user.getPassword());

            int result = ps.executeUpdate();

            if(result != 1) {
                throw new SQLException();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    public User get(String id) throws ClassNotFoundException {

        User user = new User();

        String sql = "select * from users where id = ?";

        try(Connection connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));

            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return user;
    }

}
```

---

### 1.1.3 DAO 테스트 코드

이제 만들어진 코드의 기능을 검증해야 한다.

검증하고자 할 때 사용할 수 있는 가장 간단한 방법은 오브젝트

스스로 자신을 검증하도록 만들어주는 것이다.

여기서 나는 토비의 스프링에있는 테스트 방법과

현재 많이 사용하는 JUnit 을 이용하여 테스트 하는 2가지 방법을 이용할 것이다.

- Main() 을 이용한 테스트방법 - 토비의 스프링

```java
public class Main {

    public static void main(String[] args) throws ClassNotFoundException {

        UserDao dao = new UserDao();

        User user = new User();
        user.setId("whiteship");
        user.setName("백기선");
        user.setPassword("married");

        dao.add(user);

        System.out.println(user.getId() + "등록 성공");

        User user2 = dao.get(user.getId());
        System.out.println(user2.getName());

    }

}
```

- JUnit 을 이용한 테스트 코드 작성

```java
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

```
해당 테스트 코드 로그의 일부
1. insert into users (id, name, password) values('whiteship','백기선','married')

1. select * from users where id = 'whiteship'

```

위와 같이 DAO 를 테스트하여 통과가 되었다

하지만 토비의 스프링에서는 해당 코드에 여러가지 문제가 있다고 말하고 있다.

아래와 같은 생각을 하며 다음장으로가 해답을 찾아보자

- 왜? 정상적으로 select 와 insert 를 실행하는지 무엇이 문제일까?
- 기능은 정상적으로 동작하는데 정상적으로 동작하지 않아야 문제가 아닌가?
- 코드를 개선했을때의 장점은 무엇일까?
- 코드를 개선후에 당장 또는 미래에 주는 유익이 무엇일까?
- 해당 코드를 개선하지 않고 그대로 사용한다면 어떤 문제가 발생할까?

---

## 1.2 DAO의 분리

### 1.2.1 관심사의 분리

소프트웨어 개발에서 매번 사용자의 비즈니스와 그에 따른 요구사항은 끊임없이 바뀐다.

그런 요구사항에 따라 구현한 코드가 변할수도 있다. 해당 애플리케이션이 더 이상 사용되지

않을 때 까지 코드는 변할 수 있다.

그래서 개발자가 객체를 설계할 때 가장 염두에 둬야 할 사항은

미래의 변화를 어떻게 대비할 것인가이다

예를 들어 보자

```
-- 변경에 대한 요청

DB를 오라클에서 MySQL로 바꾸면서 웹 화면의 레이아웃을
다중 프레임 구조에서 단일 프레임에 Ajax를 적용한 구조로 바꾸고, 매출이 일어날 때
에 지난달 평균 매출액보다 많으면 감사 시스템의 정보가 웹 서비스로 전송되는 동시에 
로그의 날짜 포맷을 6자리에서 Y2K를 고려해 8자리를 바꿔라
```

라는 식으로 문제가 발생하지는 않는다.

어떤 얘기냐면 모든 변경과 발전은 한번에 한가지 관심사항에 집중해서 일어난다는 뜻이다.

예시를 하나 더 들어보자

DB암호를 변경해야 한다.

[위의 DAO 코드처럼]() DB 암호를 각 코드마다 작성하게 된다면

DB암호를 변경하기 위하여 DAO클래스 수백개를 변경 해야 한다

즉 관심이 같은 것 끼리는 모으고 관심이 다른 것은 따로 떨어져 있께 하는 것이다.

이제 이전에 만든 초난감Dao의 관심사를 분리 해보자

### 1.2.2 커넥션 만들기의 추출

UserDao의 구현된 메소드에서 관심사항을 살펴 봅시다.

**UserDao 의 관심사항**

- DB와 연결을 하는 커넥션
- DB에 보낼 SQL 문장을 담을 Statement를 만들고 실행하는 것
- 작업이 끝나면 사용한 리소스인 statment 와 connection 오브젝트를 닫아주는 부분

Connnection 을 가져오는 중복된 코드를 분리 해보겠습니다

**분리 전 코드**

```java

// 사용자 정보를 DB에 넣고 관리하는 DAO

// 해당 DAO 는 토비의 스프링에 있는 DAO 를 조금 커스텀 하였습니다.

public class UserDao {

    // Insert
    public void add(User user) throws ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");

        String sql = "insert into users(id, name, password) values(?,?,?,)";

        try(Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1,user.getId());
            ps.setString(2,user.getName());
            ps.setString(3,user.getPassword());

            int result = ps.executeUpdate();

            if(result != 1) {
                throw new SQLException();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    public User get(String id) throws ClassNotFoundException {

        Class.forName("com.mysql.jdbc.Drive");

        User user = new User();

        String sql = "select * from users where id = ?";

        try(Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));

            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return user;
    }

}

```

**분리 후 코드**

```java
public class UserDao {

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
    }

    // Insert
    public void add(User user) throws ClassNotFoundException {
        String sql = "insert into users(id, name, password) values(?,?,?,)";

        try(Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1,user.getId());
            ps.setString(2,user.getName());
            ps.setString(3,user.getPassword());

            int result = ps.executeUpdate();

            if(result != 1) {
                throw new SQLException();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    public User get(String id) throws ClassNotFoundException {
        User user = new User();

        String sql = "select * from users where id = ?";

        try(Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));

            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return user;
    }

}
```

이렇게 DB와 연결하는 Connection 의 관심사를 추출해서 분리하면

나중에라도 요구사항이 변경되어 mysql의 DB를 oracle 로 변경하더라고

각 메소드별로 DB정보를 변경해주는게 아닌 getConnection 의 하나의 메소드만 수정해주면 됩니다.

해당 메소드의 내부가 변경되었으니 다시 테스트를 해봐야한다.

작성한 테스트 코드를 실행함으로써 해당 메소드를 테스트할 수 있다.

```
2021-10-07 23:31:39.956 DEBUG 13428 --- [    Test worker] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeUpdate(ProxyPreparedStatement.java:61)
1. insert into users (id, name, password) values('whiteship','백기선','married')

2021-10-07 23:31:39.970 DEBUG 13428 --- [    Test worker] jdbc.sqlonly                             :  com.zaxxer.hikari.pool.ProxyPreparedStatement.executeQuery(ProxyPreparedStatement.java:52)
1. select * from users where id = 'whiteship'
```

로그를 확인하고 테스트코드가 성공적으로 실행된걸 확인 할 수 있다.

테스트 코드는 스프링으로 작성된 테스트코드를 실행하였습니다.

### 1.2.3 DB 커넥션 만들기의 독립

아래와 같은 상황이 발생하였다고 하자.

UserDao를 구매하려는 2개의 회사가 나왔다.

N사는 oracle DB를 사용하고

D사는 mysql DB를 사용한다.

위의경우 UserDao의 소스코드를 고객에게 제공해주고 변경이 필요하면

getConnection() 메소드를 수정하라고 할수 있지만 회사에서는 UserDao의 소스코드를

공개하고 싶지 않았다.

이런 경우에는 UserDao소스코드를 N 사와 D사에 제공해주지 않고도 고객 스스로 원하는 DB 커넥션

생성 방식을 적용 시킬 수 있을까?

**상속을 통한 확장**

방법은 존재한다 기존 UserDao 코드를 한 단계 더 분리하면 된다.

우리가 만든 UserDao 에서 메소드의 구현 코드를 제거하고

getConnection() 추상 메소드로 변경한다.

```java
public abstract class UserDao {

    public abstract Connection getConnection() throws ClassNotFoundException, SQLException;

    // Insert
    public void add(User user) throws ClassNotFoundException {
        String sql = "insert into users(id, name, password) values(?,?,?,)";

        try(Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1,user.getId());
            ps.setString(2,user.getName());
            ps.setString(3,user.getPassword());

            int result = ps.executeUpdate();

            if(result != 1) {
                throw new SQLException();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    public User get(String id) throws ClassNotFoundException {
        User user = new User();

        String sql = "select * from users where id = ?";

        try(Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setPassword(rs.getString("password"));

            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
        return user;
    }

}
```

N 사 코드

```java
public class NUserDao extends UserDao {

    // Mysql
    @Override
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
    }
}
```

D 사 코드

```java
public class DUserDao extends UserDao {

    //oracle
    @Override
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.oracle.jdbc.Driver");
        return DriverManager.getConnection("jdbc:oracle://localhost:springbook", "username","password");
    }
}
```

이제 해당 회사들에게 납품을 할때 UserDao 를 상속하여

getConnection 메소드를 재구현 해달라고 요청하면서 납품을 하면된다.

이렇게 슈퍼 클래스에 기본적인 로직을 만들고 그기능의 일부나 추상 메소드나 오버라이딩이

가능한 protected 메소드 등으로 만든 뒤 서브클래스에서 필요에 맞게 구현하는 방법을

디자인 패턴에서 ***템플릿 메소드 패턴*** 이라고 한다.

템플릿 메소드 패턴은 스프링에서 애용되는 디자인 패턴이다

여기서 getConnection() 메소드는 Connection 타입 오브젝트를 생성한다는

기능을 정의해놓은 추상 메소드다 그리고 서브클래스에서의 getConnection()

메소드는 어떤 타입의 Connection 오브젝트를 생성할지 결정하는거라고 볼수 있다.

이렇게 서브클래스에서 구체적인 오브젝트 생성방법을 결정하게 하는것을

***팩토리 메소드 패턴*** 이라고 부르기도 한다.

그래도 아직 Dao에는 문제점이 존재한다.

- 자바는 클래스의 다중상속을 허용하지 않는다.  후에 다른목적으로 UserDao 에 상속할수가 없다.
- 슈퍼 클래스가 변경된다면 모든 서브클래스를 수정해야한다
- UserDao외의 Dao 클래스들이 계속 만들어진다며 getConnection()의 코드가 매 DAO 클래스마다 중복되는 심각한 문제가 발생할 것이다.

---

## 1.3 DAO의 확장

### 1.3.1 클래스의 분리

관심사가 다르고 변화의 성격이 다른 두 가지 코드를 좀더 분리해보겠다.

두 개의 관심사를 본격적으로 독립시키면서 동시에 손쉽게 확장할 수 있는 방법을 알아보자.

이번에는 아예 상속관계도 아닌 완전히 독립적인 클래스로 만들어 보겠다.

방법은 간단하다 DB 커넥션과 관련된 부분을 서브클래스가 아니라 아예 별도의 클래스에 담는다.

그리고 이 클래스를 UserDao가 이용하게 하면된다.

코드를 구현해보자

```
public class UserDao {

    private SimpleConnectionMaker simpleConnectionMaker;

    public UserDao() {
        this.simpleConnectionMaker = new SimpleConnectionMaker();
    }

    // Insert
    public void add(User user) throws ClassNotFoundException {
        Connection connection = simpleConnectionMaker.makeNewConnection()
				//...code
    }

    public User get(String id) throws ClassNotFoundException {
        Connection connection = simpleConnectionMaker.makeNewConnection()
				//...code
    }

}
```

```java
public class SimpleConnectionMaker {

    public Connection makeNewConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
    }

}
```

기존 코드에 많은 수정을 했지만 기능에 변화를 준거는 아니다.

테스트코드를 실행해도 코드가 통과한다.

이제는 완벽한 관심사 분리라고 생각할수도 있지만

결함이 생겼다. N 사와 D 사에 UserDao 클래스 납품을 할수 없게된다.

왜냐하면 UserDao의 코드가 SimpleConnectionMaker라는 특정 클래스에 종속되었기 때문이다.

어떻게 하면 이 문제를 해결 할지에 대하여 생각하며 다음장을 봐보자.

### 1.3.2 인터페이스의 도입

그렇다면 클래스를 분리하면서도 이런 문제를 해결할 수는 없을까? 물론 있다.

두 개의 클래스가 서로 긴밀하게 연결되어 있지 않도록 중간에 추상적인 느슨한 연결고리를 만들어 주는 것이다.

***추상화란 어떤 것들의 공통적인 성격을 뽑아내어 이를 따로 분리해내는 작업이다***

자바가 추상화를 위해 제공하는 가장 유용한 도구는 바로 인터페이스다.

인터페이스를 통해 접근하게 되면 실제 구현 클래스를 바꿔도 신경 쓸 일이 없다.

이제 인터페이스를 적용 시켜 보자

```java
public interface ConnectionMaker {
    
    Connection makeConnection() throws ClassNotFoundException, SQLException;

}
```

```java
public class DConnectionMaker implements ConnectionMaker {

    @Override
    public Connection makeConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.oracle.jdbc.Driver");
        return DriverManager.getConnection("jdbc:oracle://localhost:springbook", "username","password");
    }
}
```

```java
public class NConnectionMaker implements ConnectionMaker {
    
    @Override
    public Connection makeConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:springbook", "username","password");
    }
}
```

```
public class UserDao {

		private ConnectionMaker connectionMaker;

    public UserDao() {
        this.connectionMaker = new DConnectionMaker();
    }

    // Insert
    public void add(User user) throws ClassNotFoundException {
        Connection connection = connectionMaker.makeConnection()
				//...code
    }

    public User get(String id) throws ClassNotFoundException {
        Connection connection = connectionMaker.makeConnection()
				//...code
    }

}
```

위와 같이 인터페이스와 그 인터페이스를 상속받은 클래스를 만들어주고

UserDao 에서는 구현클래스를 생성해주면 된다.

하지만 UserDao의 생성자 코드를 보면 DConnection 을 생성하는 코드가 보인다.

그렇다면 다른 회사에서 사용하려면 또 UserDao를 변경해야하는 문제점이 생긴다.

다음장으로 넘어가서 보다 완벽하게 관심사를 분리해보자.

### 1.3.3 관계설정 책임의 분리

UserDao와 ConnectionMaker라는 두 개의 관심을 인터페이스를 써가면서까지 거의 완벽하게

분리했는데도, 왜 UserDao 가 인터페이스 뿐 아니라 구체적인 클래스까지 알아야 한다는 것일까?

왜냐하면 현재 UserDao 안에는 분리되지 않은 또 다른 관심사항이 존재하고 있기때문이다.

UserDao에는 지금까지 해온 관심사는 존재하지 않는다.

이미 다른 관심사는 모두 해결했기 때문이다.

현재 UserDao에 존재하는 관심사는 ConnectionMaker의 특정 구현 클래스 사이의 관계를 설정해주는 것에 대한 관심많이 존재하고 있다.

즉. 어떠한 구현체를 사용할 것인지를 지정해주는 관심사가 존재한다.

**그러면 어떻게? 해결할 것인가**

UserDao를 사용하는 클라이언트가 적어도 하나는 존재할 것이다.

여기서 말하는 클라이언트란 브라우저나 pc같은 클라이언트 장비를 말하는게 아니다

두개의 오브젝트가 있고  한 오브젝트가 다른 오브젝트의 기능을 사용한다면

사용되는 오브젝트를 서비스, 사용하는 오베젝트를 클라이언트라고 부를 수 있다.

UserDao의 클라이언트라고 하면 UserDao를 사용하는 오브젝트를 가리킨다.

**해결!**

UserDao의 클라이언트에서 UserDao를 사용하기 전에 먼저 UserDao가 어떤 ConnectionMaker의 구현 클래스를 사용할지 결정하도록 하자.

즉 UserDao 내부에서 어떤 Connection 을 사용할지 결정하지 않고

메소드 파라미터나 생성자 파라미터를 이용해 전달받아 외부에서 오브젝트를 전달 받자!

이제 코드를 작성해보자.

변경된 UserDao

```
public class UserDao {

		private ConnectionMaker connectionMaker;

    public UserDao(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker; // 클라이언트에게 책임을 넘긴다.
    }

    // Insert
    public void add(User user) throws ClassNotFoundException {
        Connection connection = connectionMaker.makeConnection()
				//...code
    }

    public User get(String id) throws ClassNotFoundException {
        Connection connection = connectionMaker.makeConnection()
				//...code
    }

}
```

```
// 클라이언트 코드

public class UserDaoTest {

    public static void main(String[] args) throws ClassNotFoundException {
				
				// 클라이언트가 책임을 지고 어떤 커넥션을 쓸지 선택.
        ConnectionMaker connectionMaker = new NConnectionMaker();

        UserDao dao = new UserDao(connectionMaker);

        User user = new User();
        user.setId("whiteship");
        user.setName("백기선");
        user.setPassword("married");

        dao.add(user);

        System.out.println(user.getId() + "등록 성공");

        User user2 = dao.get(user.getId());
        System.out.println(user2.getName());
    }

}
```

위와같이 변경되었다

UserDao를 사용하는 클라이언트 UserDaoTest에서 자바의 다형성을 이용하면서

어떤 Connection 을 사용할지 책임을 져주기 떄문에 UserDao에서는 나머지 로직에만 집중할 수 있게 된다.

이제 납품받는 회사에 따라 Connection 이 변경되어도 UserDao 와 ConnectionMaker 는 신경을 쓰지 않아도 되고 UserDao의 내부가 변경되어도 클라이언트는 사용만 할 수 있게 된다.

### 1.3.4 원칙과 패턴

**객체 지향 설계 원칙 (SOLID)**

1. 단일 책임 원칙 (SRP Single Responsibility Principle)
    - 한 클래스는 하나의 책임만 가져야 한다.
2. 개방 폐쇄 원칙 (OCP , Open-Closed Principle)
    - 클래스나 모듈은 확장에서 열려 있어야하고 변경에는 닫혀 있어야한다.
3. 리스코프 치환 원칙 (Liskov substitution principle)
    - "프로그램의 객체는 프로그램의 정확성을 깨뜨리지 않으면서 하위 타입의 인스턴스로 바꿀 수 있어야 한다." 계약에 의한 설계를 참고하라
4. 인터페이스 분리 원칙 (Interface segregation principle)
    - 특정 클라이언트를 위한 인터페이스 여러 개가 범용 인터페이스 하나보다 낫다
5. 의존관계 역전 원칙 (Dependency inversion principle)
    - 프로그래머는 “추상화에 의존해야지, 구체화에 의존하면 안된다.” 의존성 주입은 이 원칙을 따르는 방법 중 하나다.

UserDao는 DB 연결 방법이라는 기능을 확장하는데는 열려있다.

UserDao에 전혀 영향을 주지 않고도 얼마든지 기능을 확장할 수 있게 되어 있다.

동시에 UserDao 자신의 핵심 기능을 구현한 코드는 그런 변화에 영향을 받지 않고

유지할 수 있으므로 변경에는 닫혀 있따고 말할 수 있다.

**높은 응집도와 낮은 결함도**

**높은 응집도**

개방 폐쇄 원칙은 높은 응집도와 낮은 결합도라는 소프트웨어 개발의 원리로도 설명이 가능하다.

응집도가 높다는건 하나의 모듈 클래스가 하나의 책임 또는 관심사에만 집중되어 있다는 뜻이다.

즉 변경이 일어날때 모듈의 많은 부분이 함께 변한다면 응집도가 높다고 말할 수 있다

**낮은 결함도**

낮은 결함도란 책임과 관계사가 다른 오브젝트 또는 모듈과는 느슨하게 연결된 형태를 유지하는 것이다. 즉, 관계를 유지하는데 최소한의 방법만 간접적인 형태로 제공하고 나머지는 독립적인 형태로

알필요도 없게 만들어주는 것이다.

결론으로 Connection 이 변경되도 UserDao는 신경을 안써도되는 구조가 낮은 결함도다

---

## 1.4 제어와 역전

### 1.4.1 오브젝트 팩토리

지금까지 UserDao 를 깔끔하게 리팩토링을 시켰지만 아직 부족한게 남아있다.

바로 클라이언트인 UserDaoTest다. UserDaoTest 는 Dao 를 테스트하는 역할만 담당해야한다.

하지만 현재 ConnectionMaker를 생성하는 역할까지 담당하고있다.

그러니 이것도 분리하도록하자.

**팩토리**

객채의 생성 방법을 결정하고 만들어진 오브젝트를 돌려주는 클래스를 만들자

이런 역할을 하는 오브젝트를 흔히 팩토리라고 부른다.

클래스를 생성해보자

**팩토리 클래스**

```java
public class DaoFactory {

    public UserDao userDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        return new UserDao(connectionMaker);
    }

}
```

이제 UserDaoTest를 변경하도록 하자

```java
public class UserDaoTest {

    public static void main(String[] args) throws ClassNotFoundException {

        UserDao dao = new DaoFactory().userDao();

        User user = new User();
        user.setId("whiteship");
        user.setName("백기선");
        user.setPassword("married");

        dao.add(user);

        System.out.println(user.getId() + "등록 성공");

        User user2 = dao.get(user.getId());
        System.out.println(user2.getName());
    }

}
```

이제 UserDaoTest는 UserDao가 어떻게 생성되는지 만들어지는지 초기화되어있는지에 대해 신경쓰지 않아도된다. 즉 **단일 책임 원칙**을 거스르지 않게 된다.

### 1.4.2 오브젝트 팩토리의 활용

DaoFactory에 UserDao 가 아닌 다른 Dao 의 생성기능을 넣으면 어떻게 될까?

```java
public class DaoFactory {

    public UserDao userDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        return new UserDao(connectionMaker);
    }
    
    public AccountDao accountDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        return new AccountDao(connectionMaker);
    }

    public MessageDao accountDao() {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        return new MessageDao(connectionMaker);
    }

}
```

이렇게된다면 새로운 문제가 발생한다

ConnectionMaker 구현 클래스의 오브젝트를 생성하는 코드가 메소드마다 반복된다는 것이다.

중복문제를 해결하려면 분리를 시키는게 가장 좋은 방법이다

전에했던 UserDao 에서 Connection 을 분리하던것처럼 이번에도 분리하자.

```java
public class DaoFactory {

    public UserDao userDao() {
        return new UserDao(connectionMaker());
    }

    public AccountDao accountDao() {
        return new AccountDao(connectionMaker);
    }

    public MessageDao accountDao() {
        return new MessageDao(connectionMaker);
    }

    public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
    }

}
```

### 1.4.3 제어권의 이전을 통한 제어관계 역전

제어관계 역전에 대해서 알아보자 제어관계 역전이란
간단히 프로그램의 제어 흐름구조가 뒤바뀌는 것이라고 할 수 있다.

일반적으로의 흐름은 아래와 같다.

1. main() 메소드와 같이 시작점에서 사용할 오브젝트를 결정
2. 결정한 오브젝트를 생성하고
3. 만들어진 오브젝트에 있는 메소드를 호출하고
4. 그 오브젝트 메소드 안에서 다음에 사용할 것을 겾어하고 호출

이런 플로우대로 흐름이 진행된다.

제어의 역전이란 이 흐름을 완전히 뒤바꾸는 것이다.

제어의 역전에서는 오브젝트가 자신이 사용할 오브젝트를 스스로 선택하지 않는다.

또 자신이 어떻게 만들어지고 어디서 사용되는지 알 수 없다.

모든 제어 권한을 자신이 아닌 다른 대상에게 위임하기 때문이다.

예시를 들어보자

```
서블릿을 생각해보자 일반적인 자바 프로그램은 main() 안에서 

개발자가 정한 순서대로 실행이되지만

서블릿을 개발해서 서버에 배포할 수는 있찌만 그 실행을 개발자가 제어할 수 있는 방법은 없다.

대신 서블릿에 대한 제어 권한을 가진 컨테이너가 적절한 시점에 서블릿 클래스의 오브젝트를 만들고

그 안의 메소드를 호출한다. 
이렇게 서블릿이나 JSP EJB 처럼 컨테이너 안에서 동작하는 구조에 
제어의 역전 개념이 적용되어 있는 것이다.
```

```
우리가 만든 초난감 DAO 개선 작업에서도 초기에 적용했떤 템플릿 메소드 패턴을 생각해보자

추상 UserDao를 상속한 서브클래스는 getConnection() 을 구현한다.

하지만 이 메소드가 언제 어떻게 사용될지 자신은 모른다.  
단지 DB 커넥션을 만드는 기능만 구현해놓으면 슈퍼클래스에서 필요할 때 호출해서 사용하는 것이다.
```

```
우리가 만든 USerDao 에도 DaoFactory에도 제어의 역전이 적용되어있다.
원래 ConnectionMaker의 구현 클래스를 결정하고 오브젝트를 만드는 제어권은
UserDao에게 있었다. 그런데 지금은 DaoFactory에게 있다.
자신이 어떤 ConnectionMaker 구현 클래스를 만들고 사용할지를 결정할 권한을
DaoFactory에게 넘겼으니 UserDao는 이제 수동적인 존재가 됐다.

UserDaoTest는 DaoFactory가 만들고 초기화해서 공급해주는 ConnectionMaker를
사욯할 수 밖에없다. 더욱이 UserDao 와 ConnectionMaker를 생성하는 책임도 DaoFactroy가 맡고있다.
이것이 제어의 역전 (IOC) 다.

1. UserDao 도 이제 능동적으로 Connection을 선택하는게아닌 수동적이게되었다.
2. UserDaoTest도 DaoFactory가 제공해주는것을 사용할수 밖에 없는 존재가 되었따.
```

즉 제어권은 상위 템플릿 메소드에 넘기고 자신은 필요할 떄 호출되어 사용되도록 한다는,

제어와 역전 개념을 발견할 수 있다.

제어와 역전에서는 프레임워크 또는 컨테이너와 같이 애플리케이션 컴포넌트의 생성 관계썰정, 사용

생명주기를 관리 관장 해줄 존재가 필요하다.

스프링은 IoC를 극한으로 적용하고있는 프레임워크다

다음장부터 스프링이 제공하는 IoC에 대해 살펴보자.

---

## 1.5 스프링의 IoC

스프링은 다양한 영역과 기술에 관여한다.

그리고 매우 많은 기능을 제공한다. 하지만 스프링의 핵심을 담당하는건 바로

***빈팩토리 또는 어플리케이션 컨텍스트***

라고 불리는 것이다.

### 1.5.1 오브젝트 팩토리를 이용한 스프링 IoC

이제 스프링에서 DaoFactory를 사용이 가능하도록 변신시켜보자.

- Bean

  스프링이 제어권을 가지고 직접 만들고 관계를 부여하는 오브젝트

  스프링 빈은 스프링 컨테이너가 생성과 관계썰정, 사용 등을 제어해 주는 오브젝트

- Bean Factory

  빈의 생성과 관계썰정 같은 제어를 담당하는 IoC 오브젝트 빈

- Application Context

  bean Factory를 좀더 확장한 오브젝트

  빈의 생성 관계설정 제어 작업을 총괄


이제 DaoFacotry 스프링의 빈 팩토리갓 ㅏ용할 수 있는 설정정보로 만들어보자.

**DaoFactory를 사용하는 Application Context**

```java
@Configuration // 애플리케이션 컨텍스트 또는 빈 팩토리가 사용할 설정 정보라는 표시
public class DaoFactory {

    @Bean // 오브젝트 생성을 담당하는 IoC용 메소드라는 표시
    public UserDao userDao() {
        return new UserDao(connectionMaker());
    }
    
    @Bean
    public ConnectionMaker connectionMaker() {
        return new DConnectionMaker();
    }

}
```

이제 DaoFactory를 설정정보로 사용하는 ApplicationContext 를 만들어보자

애플리케이션 컨텍스트는 ApplicationContext (Inteface) 타입의 오브젝트다

그래서 구현된 클래스가 필요하다

ApplicationContext를 구현한 클래스는 여러가지가 있는데 DaoFactory 처럼 @Configuration 이 붙은 자바 코드 설정 정보를 사용하려면 AnnotationConfigApplicationContext를 이용하면 된다.

이제 코드로 UserDao를 호출해보자.

```java
public class UserDaoTest {
	public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(DaoFactory.class);
        UserDao dao = applicationContext.getBean("userDao", UserDao.class);
        
    }
}
```

이제 ApplicationContext에 등록된 Bean을 getBean() 메소드로 불러올 수 있다.

아래는 AnnotationConfigApplicationContext의 관계도다

나중에 궁금하다면 자세히 찾아보도록 하자.

![Untitled](1%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8B%E1%85%A9%E1%84%87%E1%85%B3%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%E1%84%8B%E1%85%AA%20%E1%84%8B%E1%85%B4%E1%84%8C%E1%85%A9%E1%86%AB%E1%84%80%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A8%206e6b2879c229410f896342eb6773ca5a/Untitled.png)

그런데 스프링을 적용했지만 앞에서 만든 DaoFactory를 직접 사용한것과 다를 바 없다.

다음 장에서 더욱 자세히 알아보도록 하자.

### 1.5.2 애플리케이션 컨텍스트의 동작방식

스프링의 대표적인 오브젝트 ApplicationContext 동작방식과 역할에 대해 알아보지

기존의 DaoFactory가 UserDao를 비롯한 DAO 오브젝트를 생성하고 DB 생성 오브젝트와 관계를 맺어주는 제한적인 역할을 하는데 반해

ApplicationContext는 애플리케이션에서 IoC를 적용해서 관리할 모든 오브젝트에 대한

생성과 관계설정을 담당한다.

@Configuration이 붙은 DaoFactory는 이 애플리케이션 컨텍스트가 활용하는 IoC 설정 정보다

내부적으로는 애플리케이션 컨텍스트가 DaoFactory의 userDao() 메소드를 호출해서 getBean()으로 요청할때 전달해준다.

![Untitled](1%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8B%E1%85%A9%E1%84%87%E1%85%B3%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%E1%84%8B%E1%85%AA%20%E1%84%8B%E1%85%B4%E1%84%8C%E1%85%A9%E1%86%AB%E1%84%80%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A8%206e6b2879c229410f896342eb6773ca5a/Untitled%201.png)

애플리케이션 컨텍스트를 사용했을 때 얻을 수 있는 장점은 다음과 같다.

- **클라이언트는 구체적인 팩토리 클래스를 알 필요가 없다.**

  DaoFactory 말고 다른 IoC를 적용한 오브젝트가 무수히 많이 추가되면

  클라이언트는 필요한 오브젝트를 가져오려면 어떤 팩토리 클래스를 사용해야 할지 알아야하고

  필요할 때마다 팩토리 오브젝트를 생성해야 한다.

  하지만 애플리케이션 컨텍스트를 사용한다면 아무리 오브젝트 팩토리가 많아져도

  이를 알 필요가 없이 일관된 방식으로 오브젝트를 가져 올 수 있다.


- **애플리케이션 컨텍스트는 종합 IoC 서비스를 제공해준다.**

  오브젝트가 만들어지는 방식, 시점 전략을 다르게 가져갈수 있고

  부가적으로 자동생성 오브젝트에 대한 후처리 (close), 정보의 조합, 설정, 방식의 다변화,

  인터셉팅등 다양한 기능을 활용 할 수 있다

- **애플리케이션 컨텍스트는 빈을 검색하는 다양한 방법을 제공한다.**

  타입만으로 빈을 검색하거나 특별한 애노테이션 설정이 되어 있는 빈을 찾을 수도 있다.


### 1.5.3 스프링 IoC의 용어 정리

- Bean

  스프링이 IoC 방식으로 관리하는 오브젝트

- BeanFactory

  스프링의 IoC를 담당하는 핵심 컨테이너

  등록, 생성, 조회 그 외 부가적인 빈 관리를 담당한다.

- Application Context

  빈 팩토리를 확장한 IoC 컨테이너

  빈 팩토리의 기능과 스프링이 제공하는 애플리케이션 모든 지원 기능을 담당한다.

- Configuration Metadata

  스프링의 ApplicationContext 또는 BeanFactory가 IoC를 적용하기 위해 사용하는 메타정보가 담겨있다.

- Container 또는 IoC Container

  ApplicationContext 나 BeanFactory를 가리킨다.


---

## 1.6 싱글톤 레지스트리와 오브젝트 스코프

우리가 만든 DaoFactory의 userDao() 메소드를 호출하면 호출할때마다

새로운 오브젝트를 만들어준다.

아래의 코드를 실행해보자

```
DaoFactory factory = new DaoFactory();
UserDao dao1 = factory.userDao();
UserDao dao2 = factory.userDao();

System.out.println(dao1);
System.out.println(dao2);
```

출력된 값은 매번 달라진다.

즉 새로운 오브젝트를 계속 만든다는거다.

스프링 컨텍스트로 가져온 UserDao 를 출력해보자.

```
ApplicationContext applicationContext = new AnnotationConfigApplicationContext(DaoFactory.class);
UserDao dao3 = applicationContext.getBean("userDao", UserDao.class);
UserDao dao4 = applicationContext.getBean("userDao", UserDao.class);

System.out.println(dao3);
System.out.println(dao4);
SYstem.out.println(dao3 == dao4); // true
```

같은 주소값이 나온다.

스프링은 여러번에 걸쳐 빈을 요청하더라도 매번 동일한 오브젝트를 돌려준다.

왜그럴까?

### 1.6.1 싱글톤 레지스트리로서의 애플리케이션 컨텍스트

스프링에서는 별다른 설정을 하지 않으면 내부에서 생성하는 빈 오브젝트를

모두 싱글톤으로 만든다.

그러면 왜? 스프링은 싱글톤으로 빈을 만드는 것일까?

```
스프링이 주로 적용되는 대상이 자바 엔터프라이즈 기술을 사용하는 서버환경이기 때문이다.
스프링이 처음 설계됐던 대규모의 엔터프라이즈 서버환경은
서버 하나당 최대로 초당 수십에서 수백 번씩 브라우저나 여타 시스템으로부터의
요청을 받아 처리할 수 있는 높은 성능이 요구되는 환경이다

대규모 트래픽을 위해 만들어진 환경인데
매번 클라이언트에서 요청이 올 때마다 각 로직에 관여되는 오브젝트를
새로 만들어서 사용한다면 서버가 감당하기 힘들어진다.

그래서 스프링은 빈을 싱글톤으로 관리하는 전략을 채택했다.
```

**싱글톤 패턴의 한계**

자바에서 싱글톤을 구현하는 방법이다.

- 클래스 밖에서는 오브젝트를 생성하지 못하도록 단 하나의 생성자만 private로 생성한다.
- 생성된 싱글톤 오브젝트를 저장할 수 있는 자신과 같은 타입의 스태틱 필드를 정의한다.
- 스태틱 팩토리 메소드인 getInstance()를 만들고 이 메소드가 최초로 호출되는 시점에서 한번만 오브젝트가 만들어지게 한다.
- 한번 오브젝트가 만들어지고 난 후에는 getInstance(0 메소드를 통해 이미 만들어진 객체를 넘겨준다.

일반적인 싱글톤 패턴의 단점

- pirvate 생성자를 갖고 있기 때문에 상속할 수 없다.
- 싱글톤은 테스트하기가 힘들다.
- 서버 환경에서는 싱글톤이 하나만 만들어지는 것을 보장하지 못한다.
- 싱글톤의 사용은 전역 상태를 만들 수 있기 때문에 바람직하지 못하다.

**싱글톤 레지스트리**

스프링은 서버환경에서 싱글톤이 만들어져서

서비스 오브젝트 방식으로 사용되는 것을 적극 지지한다.

자바의 기본적인 싱글톤 패턴 구현방식은 단점이 있기 때문에

스프링은 직접 오브젝트를 만들고 관리하는 기능을 제공한다 그것이 바로

***싱글톤 레지스트리*** 다 스프링 컨테이너는 싱글톤을 생성하고 관리하고 공급하는

싱글톤 관리 컨테이너이기도 하다.

스프링 싱글톤의 장점

- 스택틱 메소드와 private 생성자를 사용않는다.
- 평범한 자바 클래스를 싱글톤으로 활용하게 해준다
- 스프링이 지지하는 객체지향적인 설계 방식, 디자인패턴등을 적용하는데 제약이 사라진다.

### 1.6.3 스프링 빈의 스코프

스프링이 관리하는 오브젝트, 즉 빈이 생성되고, 존재하고, 적용되는 범위를 스프링에서는

이것을 빈의 스코프라고 한다.

스프링의 기본 스코프는 싱글톤이다

스프링의 스코프 종류에 대해 알아보자

- 싱글톤 ( 기본값)

  똑같은 주소값을 가진 객체를 리턴한다.

- 프로토타입

  싱글톤과 달리 요청마다 새로은 오브젝트를 만든다.

- Request

  HTTP 요청이 생길 때마다 생성된다.

- Sesseion

---

## 1.7 의존관계 주입(DI)

이번에는 스프링의 IoC에 대해 좀더 깊이 알아보자.

### 1.7.1 제어의 역전(IoC) 과 의존관계 주입

한가지 짚고 넘어가야할 것이 있다 Ioc라는 용어인데

Ioc가 매우 느슨하게 정의돼서 폭넓게 사용되는 용어라는 점이다.

때문에 스프링을 IoC 컨테이너라고만 해서는 스프링이 제공하는 기능의 특징을 명확하게 설명하지 못한다.

스프링이 서블릿 컨테이너처럼 서버에서 동작하는 서비스 컨테이너라는 뜻인지

단순히 IoC 개념이 적용된 템플릿 메소드 패턴을 이용해 만들어진 프레임워크인지

또 다른 IoC 특징을 지닌 기술이라는 것인지 파악하기 힘들다.

그래서 몇몇 사람의 제안으로 스프링이 제공하는 IoC 방식을 핵심을 짚어주는

의존관계 주입 (Dependency Injection) 이라는 좀 더 의도가

명확히 드러나는 이름을 사용하기 시작했다.

### 1.7.2 런타임 의존관계 설정

**의존관계**

의존관계란 무엇인지 생각해보자

두개의 클래스 또는 모듈이 의존관계에 있따고 말할 때는 방향성을 부여해줘야 한다.

즉 누가 누구에게 의존하는 관계에 있다는 식이어야 한다.

아래의 그림은 A가 B에 의존하고 있음을 나타낸다.

![Untitled](1%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8B%E1%85%A9%E1%84%87%E1%85%B3%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%E1%84%8B%E1%85%AA%20%E1%84%8B%E1%85%B4%E1%84%8C%E1%85%A9%E1%86%AB%E1%84%80%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A8%206e6b2879c229410f896342eb6773ca5a/Untitled%202.png)

***그렇다면 의존하고 있다는건 무슨 의미일까?***

위 이미지를 참고하자.

여기서는 B가 변하면 그것이 A에 영향을 미친다는 뜻이다.

B의 기능이 추가되거나 변경되거나, 형식이 바뀌거나 하면 그 영향이 A로 전달된다는 것이다.

만약 B에 새로운 메소드가 추가되거나 기존 메소드 형식이 바뀌면

A도 그에 따라 수정되거나 추가돼야 한다.

이렇게 사용의 관계에 있는 경우에 A와 B는 의존관계가 있다고 말 할 수 있다.

![Untitled](1%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8B%E1%85%A9%E1%84%87%E1%85%B3%E1%84%8C%E1%85%A6%E1%86%A8%E1%84%90%E1%85%B3%E1%84%8B%E1%85%AA%20%E1%84%8B%E1%85%B4%E1%84%8C%E1%85%A9%E1%86%AB%E1%84%80%E1%85%AA%E1%86%AB%E1%84%80%E1%85%A8%206e6b2879c229410f896342eb6773ca5a/Untitled%203.png)

지금까지 구현한거를 봐보자

UserDao 는 ConnectionMaker 라는 인터페이스에만 의존하고있다

따라서 ConnectionMaker 인터페이스가 변한다면 그 영향을 UserDao가 직접적으로 받게된다.

하지만 ConnectionMaker 를 구현한 DConnectionMaker 등이 바뀌거나 내부가 변해도

UserDao에 영향을 주지 않는다.

이그림과 같이 인터페이스를 통해 의존관계를 제한해주면 변경에 자유로워 지고

느슨한 의존관계를 갖게된다 느슨한 의존관계를 갖게되면

런타임시 사용할 오브젝트가 어떤 클래스로 만들 것인지 미리 알 수 가 없다.

프로그램이 시작되고 나서 UserDao 오브젝트가 만들어지고 나서 런타임 시에 의존관계를 맺는 대상, 즉 실제 사용대상인 오브젝트를 **의존 오브젝트라고 한다 ( dependent object)**

정리하자면 의존관계 주입이란 다음과 같은 세가지 조건을 충족해야 한다

- 클래스 모델이나 코드에는 런타임 시점의 의존관계가 드러나지 않는다. 그러기 위해서는 인터페이스에만 의존하고 있어야한다.
- 런타임 시점의 의존관계는 컨테이너나 팩토리 같은 제3의 존재가 결정한다.
- 의존관계는 사용할 오브젝트에 대한 레퍼런스를 외부에서 제공(주입) 해줌으로써 만들어진다.

### 의존 주입관계 응용

실제 운영에 사용할 데이터베이스는 매우 중요한 자원이다

평상시에도 항상 부하를 받고있어 개발 중에는 사용하지 말아야한다.

개발중에는 개발자 PC에 설치한 로컬 DB로 사용한다고 생각해보자.

개발이 진행되다가 어느시점에 지금까지 개발한 것을 운영서버로 배치해서 사용해야한다.

그런데 만약 DI방식을 적용하지 않았다면 로컬 DB에 대한 연결 기능이 있는 LocalDBConnectionMaker 라는 클래스를 만들고 모든 DAO에서 이 클래스의 오브젝트를 매번 생성하여 사용해야했을것이다.

이제 운영서버에 배포하게되면 모든 DAO 에서 LocalDBConnectionMaker를

Production 으로 변경해야 한다. DAO가 100개면 100군데 코드를 수정해야한다.

하지만 DI 방식을 적용했다면 아래와 같이 손쉽게 변경이 가능해진다.

```
		@Bean
    public ConnectionMaker connectionMaker() {
        return new LocalDBConnectionMaker();
    }
```

해당 코드를 배포할떄는

```
		@Bean
    public ConnectionMaker connectionMaker() {
        return new ProductDBConnectionMaker();
    }
```

위와같이 변경해서 배포한다면

DAO가 100개든 1000개든 상관없이 한줄만 변경하면 된다.