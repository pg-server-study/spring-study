[노션을 참고하면 더욱 좋습니다.](https://near-sunscreen-c35.notion.site/3-c784bbd9eb1347d9bef0930374ca54b1)

# 3장 템플릿

1장에서는 초난감 DAO 코드에 DI를 적용해나가는 과정을 통해서 관심이 다른 코드를 다양한 방법으로 분리하고,

확장과 변경에 용이하게 대응할 수 있는 설계구조로 개선하는 작업을 했다.

확장에는 열려있고 변경에는 굳게 닫혀 있다는 객체지향 원리중 하나인

개방 패쇄 원칙 **OCP** 를 생각 해보자. 이 원칙은 코드에서 어떤 부분은 변경을 통해 그 기능이 다양해지 확장하려고 하는 성질이 있고, 어떤 부분은 고정되어 있고 변하지 않으려는 성질이 있음을 말해준다.

변화의 특성이 다른 부분을 구분해주고, 각각 다른 목적과 다른 이유의 의해 독립적으로 변경 될 수 있는

효율적인 구조를 만들어 주는 것이 바로 이 개방 패쇄 원칙이다.

템플릿이란 이렇게 바뀌는 성질이 다른 코드 중에서 변경이 거의 일어나지 않으며

일정한 패턴으로 유지되는 특성을 가진 부분을 자유롭게 변경되는 성질을 가진 부분으로 독립시켜서

효과적으로 활용할 수 있도록 하는 방법이다.

3장에서는 스프링에 적용된 템플릿 기법을 살펴보고, 이름 적용해 보는 DAO 코드를 만드는 방법을 알아보자.

---

## 3.1 다시 보는 초난감 DAO

UserDao의 코드에는 문제점이 남아있다 여러가지 개선을 했지만 다른 심각한 문제는

바로 예외상황에 대한 처리다.

### 3.1.1 예외 처리 기능을 갖춘 DAO

DB 커넥션이라는 제한적인 리소스를 공유해 사용하는 서버에서 동작하는

JDBC 코드에는 반드시 지켜야 할 원칙이 있다. 바로 예외처리이다

정상적인 JDBC 코드의 흐름을 따르지 안혹 중간에 어떤 이유로든 예외가 발생해도

리소스를 반드시 반환하도록 해야한다. deleteAll() 메소드를 살펴보자

```
//모든 데이터 삭제
public void deleteAll() throws SQLException {
	Connection c = dataSource.getConnection();
	PreparedStatemet ps = c.prepareStatement("delete from users");
	ps.executeUpdate(); // 위 줄과 요줄에서 예외가 발생하면 바로 메소드 실행이 중단된다.
	
	ps.close();
	c.clese();
}

```

이 메소드는 Connection 과 PreparedStatement 라는 두 개의 공유 리소스를 가져와서 사용한다.

물론 정상적으로 처리되면 메소드를 마치기 전에 각각 ㅊlose()를 호출해 리소스를 반환한다.

이때 문제는 Connection 과 PreparedStatement 의 close() 메소드가 실행되지 않아서 제대로

리소스가 반환되지 않을 수 있다는 점이다.

일반적으로 서버에서 제한된 개수의 DB 커넥션을 만들어 재사용 가능한 풀로 관리한다. ( 커넥션 풀 )

getConnection() 으로 가져간 커넥션을 명시적으로 close() 해서 돌려줘야 다음 커넥션 요청때 풀에서 꺼내 쓸수 있다.

하지만 에러 처리가 없는경우 반환되지 않은 Connection이 쌓여서 어느순간부터 Connection이

쌓이면서 커넥션이 부족하여 리소스 부족 에러가 발생할 수도 있다.

```
이 문제는 진짜 중요한 문제다
이런 경험을 한적이 있다. 위와는 다른 문제지만.

Connection 반환이 정상적으로 처리되지 않아 기존에 있는 데이터가 남아있고
다음 사용자에게 기존 사용자의 정보가 보여지는 문제가 발생한걸 본적이있다.

그러므로 해당 문제를 간단하게 넘기지말고 중요하게 봐야한다.
```

해당 문제를 개선해보자.

```java
@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private DataSource dataSource;

    public void deleteAll() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement("delete from users");
            preparedStatement.execute();

        } catch (SQLException e) {
            throw e;
        } finally {
            if(preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {

                }
            }
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    } // end deleteAll
}
```

이제 예외 상황에서도 안전한 코드가 됐다. finally 로 인하여 try 블록을 수행한 후에 예외가 발생하든

정상적으로 처리되든 상관없이 close 를 해준다.

이 코드를 나는 조금 더 개선해보겠다

자바에 있는 **Try With Resources** 를 이용하면 좀더 간결하고 보기 좋은 코드가 된다

```java
@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private DataSource dataSource;

    public void deleteAll() throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("delete from users")
        ) {
            preparedStatement.execute();
        } catch (SQLException e) {
            throw e;
        }
    } // end deleteAll
}
```

어마어마한 차이가 보인다. 한눈에 들어올정도로 보기좋은 코드가 되었다

**Try With Resources** 에 대해 좀 더 알고 싶다면 아래의 사이트를 참고하자

[http://tutorials.jenkov.com/java-exception-handling/try-with-resources.html](http://tutorials.jenkov.com/java-exception-handling/try-with-resources.html)

---

## 3.2 변하는 것과 변하지 않는 것

### 3.2.1 JDBC try/catch/finally 코드의 문제점

이제 try/catch/finally 블록도 적용돼서 완성도 높은 DAO 코드가 되었지만

코드를 보면 한숨이 나온다 코드가 복잡하고

블록도 2중으로 중첩되며, 메소드마다 반복된다

이런 코드를 작성할때 효과적인방법은 Copy&Paste 신공이다

하지만 이런 방법으로 하다가 Copy가 잘못되어 finally의 close 문이 하나 빠져도

서버는 잘 돌아갈 것이다 하지만 어느 순간부터는 Connection 이 부족한 현상이 올것이다.

그리고 DAO 가 무수히 많아지면 매번 그걸 변경하고 확인해야한다.

해당 방법을 해결하는것은 1장에서 살펴봤떤 것과 비슷하게 중복되는 코드를 분리하면된다.

다만 이번 코드는 DAO와 DB 연결 기능을 분리하는것과는 성격이 다르기 때문에 해결방법이 다르다.

### 3.2.2 분리와 재사용을 위한 디자인 패턴 적용

UserDao의 메소드를 개선하는 작업을 시작해보자

먼저 할일은 성격이 다른 것을 찾아내는 것이다.

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled.png?raw=true)

쿼리문을 입력하는것을 제외하고서는 나머지는 변하지 않을 것이다.

그렇다면 어떻게 분리 할 수 잇을까?

**메소드 추출**

먼저 생각해 볼 수 잇는것은 변하는 부분을 메소드로 뺴는 것이다.

```java
@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private DataSource dataSource;
    
    public void deleteAll() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = dataSource.getConnection();
            preparedStatement = makeStatement(connection);
            
            preparedStatement.execute();
        } catch (SQLException e) {
            throw e;
        } finally {
            if(preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {

                }
            }
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    }
    
    private PreparedStatement makeStatement(Connection connection) throws SQLException {
        PreparedStatement preparedStatement;
        preparedStatement = connection.prepareStatement("delete from users");
        return preparedStatement;
    }
}
```

자주 바뀌는 부분을 독립시켰는데 이득이 없어 보인다.

보통 메소드 추출 리팩토링을 적용하는 경우에는 분리시킨 메소드를 다른 곳에서 재사용할 수 있어야 하는데 이건 반대로 분리시키고 남은 메소드가 재사용이 필요한 부분이고

분리된 메소드는 DAO 마다 새롭게 만들어서 확장돼야 한다.

**템플릿 메소드 패턴의 적용**

다음은 템플릿 메소드 패턴을 이용해서 분리해보자.

템플릿 메소드 패턴은 상속을 통해 기능을 확장해서 사용하는 부분이다.

변하지 않는 부분은 슈퍼 클래스에 두고 변하는 부분은 추상 메소드로 정의해둬서 서브클래스에서 오버라이드하여 새롭게 정의해 쓰도록 하는 것이다.

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class UserDao {

    abstract protected PreparedStatement makeStatement(Connection connection) throws SQLException;

}
```

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDaoDeleteAll extends UserDao {

    @Override
    protected PreparedStatement makeStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("delete from users");
    }
}
```

이제 UserDao 클래스의 기능을 확장하고 싶을 때마다 상속을 통해 자유롭게 확장할 수 있고

확장 때문에 기존의 상위 DAO 클래스에 불필요한 변화는 생기지 않도록 할 수 있으니

객체지향 설계의 핵심 원리인 **개방 패쇄 원칙(OCP)** 을 그럭저럭 지키는 구조를 만들어낼 수는 있는 것 같다.

하지만 템플릿 메소드 패턴으로의 접근은 제한이 많다.

가장 큰 문제는 DAO로직마다 상속을 통해 새로운 클래스를 만들어야함으로

많은 클래스들이 생성된다.

**전략 패턴의 적용**

**개방 패쇄 원칙(OCP)**  을 지키는 구조이면서도 템플릿 메소드 패턴보다 유연하고

확장성이 뛰어난 것이, 오브젝트를 아예 둘로 분리하고 클래스 레벨에서는

인터페이스를 통해서만 의존하도록 만드는 전략 패턴이다.

전략 패턴은 OCP 관점에 보면 확장에 해당하는 변하는 부분을 별도의 클래스로 만들어 추상화된

인터페이스를 통해 위임하는 방식이다.

![Untitled(https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%201.png?raw=true)

좌측에 있는 Context의 contextMethod()에서 일정한 구조를 가지고 동작하다가

특정 확장 기능은 Strategy인터페이스를 통해 외부의 독립된 전략 클래스에 위임하는 것이다.

delleteAll() 메소드에서 변하지 않는 부분이라고 명시한 것이 바로 contextMethd()가 된다.

JDBC를 이용해 DB를 업데이트하는 변하지 않는 context을 갖는다

정리해보자면 다음과 같다

- DB 커넥션 가져오기
- PreparedStatement를 만들어줄 외부 기능 호출하기
- 전달받은ㄴ PreparedStatement 실행하기
- 예외가 발생하면 이를 다시 메소드 밖으로 던지기
- 모든 경우에 만들어진 PreparedStatement 와 Connection을 적절히 닫아 주기

두번째 작업에서 사용하는것을 만들어주는 외부 기능이 바로 전략패턴에서 말하는

전략이라고 볼 수 있다. 이 기능을 인터페이스로 만들어주고 인터페이스의 메소드를 통해

호출 해주면 된다.

개선해보자

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface StatementStrategy {
    PreparedStatement makePreparedStatement(Connection connection) throws SQLException;
}
```

```java
public class DeleteAllStatement implements StatementStrategy {
    @Override
    public PreparedStatement makePreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("delete from users");
    }
}
```

```java
@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private DataSource dataSource;

    public void deleteAll() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = dataSource.getConnection();
						// 전략 패턴
            StatementStrategy strategy = new DeleteAllStatement();

            preparedStatement = strategy.makePreparedStatement(connection);

            preparedStatement.execute();
        } catch (SQLException e) {
            throw e;
        } finally {
            if(preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {

                }
            }
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    }
}
```

하지만 전략 패턴은 필요에 따라 Context 는 유지되면서 전략을 바꿔 쓸 수 있다는 것인데

이렇게 이미 구체적인 전략 클래스인 DeleteAllStatement 를 사용하도록 고정하면 뭔가 이상하다.

**DI 적용을 위한 클라이언트/컨텍스트 분리**

이 문제를 해결하기 위한 전략패턴이 실제 사용방법을 좀 더 살펴보자.

전략 패턴에 따르면 Context가 어떤 전략을 사용하게 할 것인가는 Context를 사용하는 앞단의

Clinet가 결정하는게 일반적이다.

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%202.png?raw=true)

이제 클라이언트가 어떤것을 사용할지 선택하게 만들어 보자

```java

@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private DataSource dataSource;

		public void jdbcContextWithStatementStrategy(StatementStrategy strategy) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = dataSource.getConnection();

            preparedStatement = strategy.makePreparedStatement(connection);

            preparedStatement.execute();
        } catch (SQLException e) {
            throw e;
        } finally {
            if(preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {

                }
            }
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    } // end deleteAll
}
```

이제 클라이언트에게 PreparedStatement를 선택하게 만들었으니 클라이언트르 ㄹ봐보자

```
public void deleteAll() throws SQLException {
	StatementStrategy statementStrategy = new DeleteAllStatement();
	jdbcContextWithStatementStrategy(statementStrategy);
}
```

이제 구조로 볼 때 완벽한 전략 패턴의 모습을 갖췄다.

---

## 3.3 JDBC 전략 패턴의 최적화

지금까지 기존의 deleteAll() 메소드에 담겨있던 변하지 않는 부분, 자주 변하는 부분을 전략패턴을

사용해 깔끔하게 분리해냈다.

### 3.3.1 전략 클래스의 추가 정보

이번에 add() 메소드에도 적용해보자.

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddStatement implements StatementStrategy {
    @Override
    public PreparedStatement makePreparedStatement(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("insert into users(id, name, password) values(?,?,?)");

        preparedStatement.setString(1,user.getId());
        preparedStatement.setString(2,user.getName());
        preparedStatement.setString(3,user.getPassword());
        return preparedStatement;
    }
}
```

이렇게 분리하니 컴파일 에러가난다 deleteAll() 과 달리 add() 에서는 user라는 부가적인 정보가 필요하기 때문이다.

AddStatement에 클라이언트로부터 User 타입 오브젝트를 받을 수 있도록 개선해주자.

```java
import com.example.springtoby.chapter3.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddStatement implements StatementStrategy {
    
    private User user;

    public AddStatement(User user) {
        this.user = user;
    }

    @Override
    public PreparedStatement makePreparedStatement(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("insert into users(id, name, password) values(?,?,?)");

        preparedStatement.setString(1,user.getId());
        preparedStatement.setString(2,user.getName());
        preparedStatement.setString(3,user.getPassword());
        return preparedStatement;
    }
}
```

그리고 클라이언트인 UserDao의 add() 메소드를 User 정보를 전달해 주도록 수정하자

```
public void add(User user) throws SQLException {
	StatementStrategy statementStrategy = new AddStatement(user);
  jdbcContextWithStatementStrategy(statementStrategy);
}
```

그리고 테스트 코드를 다시한번 돌려보자

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%203.png?raw=true)

성공 하는 것을 볼 수 있다.

### 3.3.2 전략과 클라이언트의 동거

지금까지 해온 작업만으로도 깔끔하게 만들긴 했지만

좀더 개선할 부분을 찾아보자.

개선점

- DAO 메소드마다 새로운 StatementStrategy 를 구현해야한다
- UserDao 때보다 클래스 파일의 개수가 늘어난다.
- User와 같은 부가정보가 있는경우 오브젝트를 전달받는 생성자와 인스턴스 변수를 만들어야 한다.

**로컬 클래스**

클래스 파일이 많아지는 문제는 간단한 해결 방법이 있따

StatementStrategy 전략 클래스를 매번 독립된 파일로 만들지 말고 UserDao 클래스 안에

내부 클래스로 정의해 버리는 것이다.

```
public void add(User user) throws SQLException {
    class AddStatement implements StatementStrategy {

        private User user;

        public AddStatement(User user) {
            this.user = user;
        }

        @Override
        public PreparedStatement makePreparedStatement(Connection connection) throws SQLException {
            PreparedStatement preparedStatement = connection.prepareStatement("insert into users(id, name, password) values(?,?,?)");

            preparedStatement.setString(1,user.getId());
            preparedStatement.setString(2,user.getName());
            preparedStatement.setString(3,user.getPassword());
            return preparedStatement;
        }
    }
    StatementStrategy statementStrategy = new AddStatement(user);
		jdbcContextWithStatementStrategy(statementStrategy);
}
```

**익명 내부 클래스**

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%204.png?raw=true)

```
public void add(User user) throws SQLException {
        StatementStrategy statementStrategy = new StatementStrategy() {
            @Override
            public PreparedStatement makePreparedStatement(Connection connection) throws SQLException {
                PreparedStatement preparedStatement = connection.prepareStatement("insert into users(id, name, password) values(?,?,?)");

                preparedStatement.setString(1,user.getId());
                preparedStatement.setString(2,user.getName());
                preparedStatement.setString(3,user.getPassword());
                return preparedStatement;
            }
        };
        
        jdbcContextWithStatementStrategy(statementStrategy);
    }
```

```
public void add(User user) throws SQLException {
        StatementStrategy statementStrategy = connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement("insert into users(id, name, password) values(?,?,?)");

            preparedStatement.setString(1,user.getId());
            preparedStatement.setString(2,user.getName());
            preparedStatement.setString(3,user.getPassword());
            return preparedStatement;
        };

        jdbcContextWithStatementStrategy(statementStrategy);
    }
```

---

## 3.4 컨텍스트와 DI

전략 패턴의 구조로 보자면 UserDao 의 메소드가 클라이언트고, 익명 내부 클래스로 만들어진 것이

개별적인 전략이고 jdbcContextWithStatementStrategy() 메소드는 컨텍스트다.

jdbcContextWithStatementStrategy() 메소드는 PreparedStatement를 실행하는 기능이므로

다른 곳에서도 사용이 가능하다 그러므로 메소드를 독립 시켜 모든 DAO 가 사용가능하게 하자

**클래스 분리**

```java
import com.example.springtoby.toby.statement.StatementStrategy;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@NoArgsConstructor
@Component
public class JdbcContext {

    @Autowired
    private DataSource dataSource;

    public void workWithStatementStrategy(StatementStrategy statementStrategy) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = this.dataSource.getConnection();

            preparedStatement = statementStrategy.makePreparedStatement(connection);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {

                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    }
}
```

```java
import com.example.springtoby.toby.context.JdbcContext;
import com.example.springtoby.toby.statement.DeleteAllStatement;
import com.example.springtoby.toby.statement.StatementStrategy;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private JdbcContext jdbcContext;

    public void deleteAll() throws SQLException {
        StatementStrategy statementStrategy = new DeleteAllStatement();
        this.jdbcContext.workWithStatementStrategy(statementStrategy);
    }

    public void add(User user) throws SQLException {
        StatementStrategy statementStrategy = connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement("insert into users(id, name, password) values(?,?,?)");

            preparedStatement.setString(1, user.getId());
            preparedStatement.setString(2, user.getName());
            preparedStatement.setString(3, user.getPassword());
            return preparedStatement;
        };
        this.jdbcContext.workWithStatementStrategy(statementStrategy);
    }

}
```

---

## 3.5 템플릿과 콜백

지금까지  UserDao와 StatementStrategy, JdbcContext를 이용해 만든 코드는 일종의

전략 패턴이 적용된 것이라고 볼 수 있다.

복잡하지만 바뀌지 않는 일정한 패턴을 갖는 작업 흐름이 존재하고

일부분만 자주 바꿔서 사용해야 하는 경우 적합한 구조다.

전략 패턴의 기본 구조에 익명 내부 클래스를 활용한 방식이며 스프링에서는

템플릿/콜백 패턴 이라고 부른다.

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%205.png?raw=true)

### 3.5.1 템플릿/콜백의 동작원리

템플릿은 고정된 작업 흐름을 가진 코드를 재사용한다는 의미에서 붙인 이름이다.

콜백은 템플릿 안에서 호출되는 것을 목적으로 만들어진 오브젝트를 말한다.

**템플릿/콜백의 특징**

여러 개의 메소드를 가진 일반적인 인터페이스를 사용할 수 잇는 전략 패턴의 전략과 달리

템플릿/콜백 패턴은의 콜백은 보통 단엘 미소드 인터페이스를 사용한다.

템플릿의 작업 흐름 중 특정 기능을 위해 한번 호출되는 경우가 일반적이기 때문이다.

하나의 템플릿에서 여러 가지 종류의 전략을 사용해야 한다면 하나 이상의

콜백 오브젝트를 사용 할 수도 있다. 콜백은 일반적으로

하나의 메소드를 가진 인터페이스를 구현한 익명 내부 클래스로 만들어진다고 보면 된다.

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%206.png?raw=true)

- 클라이언트 역할은 템플릿 안에서 실행될 로직을 담은 콜백 오브젝트를 만들고, 콜백이 참조할 정보를 제공하는 것이다. 만들어진 콜백은 클라이언트가 템플릿의 메소드를 호출할 때 파라미터로 전달된다.
- 템플릿은 정해진 작업 흐름을 따라 작업을 진행하다가 내부에서 생성한 참조정보를 가지고 콜백 오브젝트의 메소드를 호출한다. 콜백은 클라이언트 메소드에 있는 정보와 템플릿이 제공한 참조정보를 이용해서 작업을 수행하고 그결과를 다시 템플릿에 돌려준다.
- 템플릿은 콜백이 돌려준 정보를 사용해서 작업을 마저 수행한다.

**JdbcContext에 적용된 템플릿/콜백**

앞에서 만들었떤 UserDao, JdbcContext 와 StatementStrategy의 코드에 적용된 템플릿/콜백 패턴을 살펴보자

![Untitled](https://github.com/pg-server-study/spring-study/blob/JH/JH/chapter3/image/Untitled%207.png?raw=true)

JdbcContext의 workWithStatementStrategy() 템플릿은 리턴 값이 없는 단순한 구조다.

조회 작업에서는 보통 템플릿이 작업 결과를 클라이언트에 리턴해준다.

### 3.5.2 편리한 콜백의 재활용

템플릿/콜백 방식은 템플릿에 담긴 코드를 여기저기 반복적으로 사용하는 원시적인 방법에 비해 많은 장점이 있다.

당장에 JdbcContext를 사용하기만 해도 기존에 JDBC 기반의 코드를 만들었을 때 발생햇던

여러가지 문제점과 불편한 점을 제거할 수 있다.

그런데 템플릿/콜백 방식에는 한 가지 아쉬운 점이 있다. DAO 메소드에서 매번 익명 내부 클래스를

사용하기 때문에 상대적으로 코드를 작성하고 읽기가 불편하다.

**콜백의 분리와 재활용**

이번에는 복잡한 익명 내부 클래스의 사용을 최소화 할 수 있는 방법을 찾아보자.

```
private void executeSql(final String query) throws SQLException {
    this.jdbcContext.workWithStatementStrategy(
            connection -> connection.prepareStatement(query)
    );
 }

public void deleteAll() throws SQLException {
    executeSql("delete from users");
}
```

이런식으로 SQL 문만 바뀌는 멧도느는 executeSql 만 실행하면 동작하게 변경하였다

**콜백과 템플릿의 결합**

한 단계 더 나아가 보자. executeSql() 메소드는 UserDao 만 사용하기는 아깝다

이렇게 재사용 가능한 콜백을 담고 있는 메소드라면 DAO가 공유할 수 있는 템플릿 클래스 안으로

옮겨도 된다.

엄밀히 말해서 템플릿은 JdbcContext 클래스가 아니라

workWithStatementStrategy() 메소드이므로 jdbcContext 클래스로 콜백 생성과 템플릿 호출이 담긴 executeSql() 메소드를 옴긴다고 해도 문제 될 것은 없다.

```
@NoArgsConstructor
@Component
public class UserDao {

    @Autowired
    private JdbcContext jdbcContext;

    @Autowired
    private DataSource dataSource;

    public void deleteAll() throws SQLException {
        this.jdbcContext.executeSql("delete from users");
    }
		//......
}
```

```
public void deleteAll() throws SQLException {
    this.jdbcContext.executeSql("delete from users");
}
```

이제 모든 DAO 에서 executeSql() 메소드를 사용 할 수 있게 됐다.

---

## 3.6 스프링의 JdbcTemplate

템플릿과 콜백의 기본적인 원리와 동작방식, 만드는 방법을 알아봤으니 이번에는 스프링이 제공하는

템플릿/ 콜백 기술을 알아보자.

스프링이 제공하는 JDBC 코드용 기본 템플릿은 JdbcTemplate이다

앞에서 만든 JdbcContext는 버리고 스프링의 JdbcTemplate로 바꿔보자

JdbcTemplate의 초기화를 위한 코드

```
public class UserDao {
	...
	private JdbcTemplate jdbcTemplate;
	
	public void setDataSource(DataSource dataSource) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
			this.dataSource = dataSource;
	}

}
```

### 3.6.1 update()

deleteAll() 에 먼저 적용해보자

deleteAll() 에 적용했던 콜백은 StatementStrategy 인터페이스의 makePreparedStatement() 메소드다

이에 대응되는 JdbcTemplate의 콜백은 PreparedStatementCreator 인터페이스의

createPreparedStatement() 메소드다. 템플릿으로부터 Connection을 제공 받아서

PreparedStatement를 만들어 돌려준다는 면에서 구조는 동이하다.

```
public void deleteAll() throws SQLException {
    this.jdbcTemplate.update(new PreparedStatementCreator() {
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement("delete from users");
        }
    });
}
```

```
public void deleteAll() throws SQLException {
    this.jdbcTemplate.update("delete from users");
}
```

add() 메소드에 대한 편리한 메소드도 제공된다.

치환자를 가진 SQL로 PreparedStatement를 만들고 함께 제공하는 파라미터를 순서대로 바인딩

해주는 기능을가진 update() 메소드를 사용할 수 있따.

```
public void add(final User user) throws SQLException {
    this.jdbcTemplate.update("insert into users(id, name, password) values(?,?,?)", user.getId(), user.getName(), user.getPassword());
}
```

### 3.6.2 queryForInt()

getCounet() 는 SQL 쿼리를 실행하고 ResultSet을 통해 결과 값을 가져오는 코드다.

이런 작업 흐름을 가진 코드에서 사용할 수 있는 템플릿은 PreparedStatementCreator 콜백과

ResultSetExtractor 콜백을 파라미터로 받는 query() 메소드다

```
public int getCount() {
    return this.jdbcTemplate.query(new PreparedStatementCreator() {
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement("select count(*) from users");
        }
    }, new ResultSetExtractor<Integer>() {
        @Override
        public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
            rs.next();
            return rs.getInt(1);
        }
    });
}
```

```
//람다
public int getCount() {
    return this.jdbcTemplate.query(con -> con.prepareStatement("select count(*) from users"), (rs) -> {
        rs.next();
        return rs.getInt(1);
    });
}
```

더 간단하게

```
public int getCount() { // queryForInt 는 Deprecated 되었다
    return this.jdbcTemplate.queryForInt("select count(*) from users");
}

// 이방법을 사용해라.
public int getCount() {
    return this.jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
}
```

첫번째 인자인 new PreparedStatementCreator 에서 PreparedStatement를 만들고

두번째 인자인 ResultSetExtractor 에서 쿼리를 실행한후 반환한다.

### 3.6.3 queryForObject()

```
public User get(String id)  {
    return this.jdbcTemplate.queryForObject("select * from users where id = ?", new Object[]{id}, new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            return user;
        }
    });
}
```

### 3.6.4 query()

```
public List<User> getAll() {
    return this.jdbcTemplate.query("select * from users order by id", new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            return user;
        }
    });
}
```

## 3.7 정리

- JDBC와 같은 예외가 발생할 가능성이 있으며 공유 리소스의 반환이 필요한 코드는 반드시 try/catch/finally 블록으로 관리해야한다.
- 일정한 작업 흐름이 반복되면서 그중 일부 기능만 바뀌는 코드가 존재한다면 전략패턴을 적용한다. 바뀌지 않는 부분은 컨텍스트로, 바뀌는 부분은 전략으로 만들고 인터페이스를 통해 유연하게 전략을 변경할 수 있도록 구현한다.
- 같은 애플리케이션 안에서 여러 가지 종류의 전략을 다이내믹하게 구성하고 사용해야한다면 컨텍스트를 이용하는 클라이언트 메소드에서 직접 전략을 정의하고 제공하게 만든다.
- 클라이언트 메소드 안에 익명 내부 클래스를 사용해서 전략 오브젝트를 구현하면 코드도 간결해지고 메소드의 정보를 직접 사용할 수 있어서 편리하다.
- 컨텍스트가 하나 이상의 클라이언트 오브젝트에서 사용된다면 클래스를 분리해서 공유하도록 만든다
- 컨텍스트는 별도의 빈으로 등록해서 DI 받거나 클라이언트 클래스에서 직접 생성해서 아용한다. 클래스 내부에서 컨텍스트를 사용할 때 컨텍스트가 의존하는 외부의 오브젝트가 있다면 코드를 이용해서 직접 DI 해줄 수 있다.
- 단일 전략 메소드를 갖는 전략패턴이면서 익명 내부 클래스를 사용해서 매번 전략을 새로 만들어 사용하고, 컨텍스트를 호출과 동시에 전략 DI를 수행하는 방식을 템플릿/콜백 패턴이라고 한다.
- 콜백의 코드에도 일정한 패턴이 반복된다면 콜백을 템플릿에 넣고 재활용 하는 것이 편리하다
- 템플릿과 콜백의 타입이 다양하게 바뀔 수 있다면 제네릭스를 이용한다
- 스프링은 JDBC 코드 작성을 위해 JdbcTemplate을 기반으로 하는 다양한 템플릿과 콜백을 제공한다.
- 템플릿은 한 번에 하나 이상의 콜백을 사용할 수도 있고, 하나의 콜백을 여러번 호출할 수도 있다.
- 템플릿/콜백을 설계할 때는 템플릿과 콜백 사이에 주고받는 정보에 관심을 둬야 한다

```
추가 개인 의견
JdbcTemplate는 한번정도 봐두는게 좋습니다.

요즘은 Spring Data Jpa같은거로 쉽게 사용하지만 제가 최근 과제테스트에서 JdbcTemplate를
경험한적이있습니다

그때는 JdbcTemplate의 사용법을 잘 몰라 작성을 못했던 경험이 있으므로 
한번쯤은 JdbcTemplate의 메소드를을 봐보시는걸 추천드립니다.

네이버 채용 백엔드 2차 과정
```
