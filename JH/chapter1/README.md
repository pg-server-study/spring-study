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
