# 3장 템플릿

**템플릿이란 ?** 바뀌는 성질이 다른 코드 중에서 **변경이 거의 일어나지 않으며 일정한 패턴으로 유지되는 특성을 가진 부분을 자유롭게 변경이 되는 성질을 가진 부분으로부터 독립**시켜서 효과적으로 활용 할 수 있도록 하는 방법.

> 다시 보는 초난감 Dao 

UserDao의 코드에는 아직 문제점이 남아 있다.DB 연결과 관련된 여러 가지 개선 작업은했지만，

다른면에서 심각한 문제점이 있다. 바로 예외상황에 대한처리다.

```java
public void deleteAll() throws SQLException { 
	Connection c = dataSource .getConnection(); 
	
	PreparedStatement ps = c.prepareStatement("delete from users");
    
	pS .executeUpdate(); 
	ps .close(); 
	c. close() ; 
}
```

그런데 PreparedStatement를 처리히는 중에 예외가 발생하면 어떻  게 될까? 

이때는 메소드 실행을 끝마치지 못하고 바로 메소드를 빠져나가게 된다. 

이때  문제는 Connection과 PreparedStatement 의 close( ) 메소드가 실행되지 않아서 제대로  리소스가 반환되지 않을 수 있다는 점이다. 

이런 식으로 오류가 날 때마다 미처 반환되지 못한 Connection 이 계속 쌓이면  어느 순간에 커넥션 풀에 여유가 없어지고 리소스가 모자란다는 심각한 오류를 내며 서버가 중단될 수 있다.

>  예외 발생 시에도 리소스를 반환하도록 수정한 deleteAll ()

```java
public void deleteAll( ) throws SQLException { 
	Connection c = null; 
	PreparedStatement ps = null; 
    
	try { //예외가 발생할 가능성이 있는 코드를 모두 try블록으로 묶어준다.
		c = dataSource .getConnection(); 
		ps = c. prepareStatement( "delete from users" ); 
		Ps .executeUpdate ();
	} catch (SQLException e) { 
        //catch 블록을 둔다 .  아직은 예외를 다시 메소드 밖으로 던지는거 밖에 없다.
		throw e; 
	} finally { //예외가 발생했을때나 안 했을때나 모두 실행된다.
		if (ps != null) { 
			try {
				ps. close(); 
		} catch (SQLException e){
		} // close 메소드 에서도 exception이 발생 할수 있기 때문에 이를 잡아줘야한다.
	}
	if (c != null) { 
		try {
			c.close(); 
		} catch (SQLException e) {
		}
		}
	}
}
```

>JDBC 조회 기능의 예외처리 

```java
public int getCount() throws SQLException{
	Connection c = null; 
	PreparedStatement ps = null; 
	ResultSet rs = null; 
	
	try {
		c = dataSource .getConnection (); 
		
		ps = c.prepareStatement('’select count(*) frαn users' );
		
		rs = ps.executeQuery(); 
        //ResultSet도 다향한 Exception이 발생 할수 있는 코드이므로 try블록안에 둬야한다
		re.next();
		return re.getId(1);
	}catch (SQLException e) { 
		throw e; 
	} finally { 
		if (rs != null) { 
			try { 
				rs.close;		
			} catch (SQLException e){
			}
		}
		if (ps != null) { 
			try {
				ps. close(); 
			} catch (SQLException e) { 
			}
		}
		if (c != null) { 
			try {
				c. close(); 
			}catch (SQLException e){
			}
		}
	}
}
```

###  JDBC try / catch / finally 코드의 문제점

- 복잡한 try/catch/finally 블록 이중으로 중첩까지 되어 나오는데다， 모든 메소드마다 반복된다.

-  누군가 DAO 로직을 수정하려고 했을 때 복잡한 try/catch/finally 블록 안에서 필요한 부분을 찾아서 수정해야 하고， 언젠가 폭 필요한 부분을  잘못 삭제해버리면 같은 문제가 반복된다.
- 예외상황을 처리하는 코드는 테스트하기가 매우 어렵고 모든 DAO 메소드에 대해 이런 태스트를 일일이 한다는 건 매우 번거롭다.

### 분리와 재사용를 위한 디자인 패턴 적용 

> 개선할 deleteAll() 메소드

```java
	Connection c = null;  //변하지 않는 부분
	PreparedStatement ps = null; 
	
	try { 
		c = dataSource .getConnection();
		
		ps = c.prepareStatement("delete from users");  //변하는부분
			
		ps .executeUpdate(); 
	} catch (SQLException e) { //변하지 않는부분
		throw e; 
	} finally { 
		if (ps != null) { try { ps.close(); } catch (SQLException e) {} } 
		if (c != null) { try {c .close(); } catch (SQLException e) {} }
}
```

> add() 메소드에서 수정할 부분

```java
ps = c.prepareStatement("insert into users(id, name , password) values(? ,?,?)"); 
pS.setString(l , user.getld()); 
pS .setString(2, user.getName()); 
pS .setString(3, user.getPassword()); 
```

### 메소드 추출

>  먼저 생각해볼 수 있는 방법은 변하는 부분을 메소드로 빼는 것이다.

```java
public void deleteAll() throws SQLException { 
	try {
		c = dataSource.getConnection(); 
        
		ps = makeStatement(c); 
        //변하는 부분을 메소드로 추출하고 변하지 않는 부분에서 호출하도록 만들었다.
           
		ps.executeUpdate(); 
	} catch (SQLException e)
}
private PreparedStatement makeStatement(Connection c) throws SQLException { 
	PreparedStatement ps; 
	ps = c.prepareStatement("delete from users'’) ; 
	return ps;
}
```

보통 메소드 추출 리팩토링을 적용히는 경우에는 분리시킨 메소드를 다른 곳에  서 재시용할 수 있어야는데

이건 반대로 분리시키고 남은 메소드가 재사용이 필요한 부분이고， 분리된 메소드는 DAO 로직마다 새롭게 만들어서 확장돼야 동}는 부분이  기 때문이다. 뭔가 반대로 됐다.

### 템플릿 메소드 패턴의 적용

> 템플릿 메소드 패턴은 상속을 통해  기능을 확장해서 사용하는 부분이다. 
>
> 변하지 않는 부분은 슈퍼클래스에 두고 변하는 부  분은 추상 메소드로 정의해둬서 서브클래스에서 오버라이드하여 새롭게 정의해 쓰도록  하는것이다.

```java
abstract protected PreparedStatement makeStatement(Connection c) throws 
	SQLException; 
```

```java
public class UserDaoDeleteAll extends UserDao {

	protected PreparedStatement makeStatement(Connection c) throws SQLException { 
		PreparedStatement ps = c.prepareStatement("delete from users"); 
		return ps;
	}
}
```

 하지만 템플릿 메소드 패턴으로의 접근은 제한이 많다. 가장 큰 문제는 DAO 로직마다 상속을 통해 새로운 클래스를 만들어야 한다는 점이다.

### 전략패턴의 적용

> 개방 패쇄 원칙OCP을 잘 지키는 이면서도 템플릿 메소드 패턴보다 유연하고 확장성이 뛰어난 것이， 오브젝트를 아예 둘로 분리하고 클래스 레벨에서는 인터페이스  를 통해서만 의존하도록 만드는 전략 패턴이다.

```java
public interface StatementStrategy {
	PreparedStatement makePreparedStatement(Connection c) throws SQLException;
}
```

```java
public class DeleteAllStatement implements StatementStrategy {
	public PreparedStatement makePreparedStatement(Connection c) throws 
		SQLException {
			PreparedStatement ps = c.prepareStatement("delete from users"); 
			return ps;
	}
}
```

```java
public void deleteAll() throws SQLException{
    ...
	try{
		c = dataSource.getConnection(); 
        
		StatementStrategy strategy = new DeleteAllStatement(); 
		ps = strategy.makePreparedStatement(c); 
        
		ps.executeUpdate(); 
		} catch (SQLException e) {
   ...
}
```

인터페이스를 통해 전략 패턴을 적용했지만 , 이미 구체  적인 전략 클래스인 DeleteAllStatement를 사용하도록 고정되어 있다.

### DI 적용을 위한 클라이언트/컨텍스트 분리

전략 패턴에 따르면 Context 가 어떤 전략을 사용하게 할 것인가는 Context를 시용하는 앞단의 Client가 결정하는 게 일반적이다. 

Client 가 구체적인 전략의 하나를 선택하고 오브젝트로 만들어서 Context 에 전달히는 것이다.

결국 이 구조에서 전략 오브젝트 생성과 컨텍스트로의 전달을 담당히는 책임을 분리시킨 것이 바로 ObjectFactory 이며， 이를 일반화한 것이 앞에서 살펴봤던 의존관계 주입이 였다.

> 컨텍스트에 해당하는 부분은 별도의 메소드로 독립시격보자.

```java
public void jdbcContextWithStatementStrategy(StatementStrategy stmt) throws 
SQLException { 
		Connection c = null; 
		PreparedStatement ps = null; 
		
	try {
		c = dataSource .getConnection(); 
		
		ps = stmt.makePreparedStatement(c); 
		
		pS .executeUpdate(); 
	} catch (SQLException e) { 
		throw e; 
	} finally { 	
		if (ps != null) { try { ps .close(); } catch (SQLException e) {} } 
		if (c != null) { try {c.close(); } catch (SQLException e) {} }
	}
}
```

클라이언트로부터  StatementStrategy 타입의 전략 오브젝트를 제공받고 JDBC try/catch/finally  구조로 만들어진 컨텍스트 내에서 작업을 수행한다.

> 클라이언트 책임을 갖도록 재구성한 deleteAll () 메소드

```java
public void deleteAll() throws SQLException { 
	StatementStrategy st = new DeleteAllStatement();  
	jdbcContextWithStatementStrategy(st);
}
```

> add() 메소드 수정	

```java
public class AddStatement implements StatementStrategy { 
	public PreparedStatement makePreparedStatement(Connection c) 
		throws SQLException { 
	PreparedStatement ps = 
	c.prepareStatement('insert into users(id, name , password) 
	values(?,?,?)"); 
	ps.setString(l , user.getld()); 
	ps.setString(2, user.getName()); 
	ps.setString(3, user.getPassword()); 
	
		return ps;
	}
}
```

```java
public class AddStatement implements StatementStrategy { 
	User user; 
	public AddStatement(User user) { 
		this.user = user; 
	}
public PreparedStatement makePreparedStatement(Connection c) ( 
		pS.setString(l , user.getld()); 
		ps.setString(2, user.getName()); 
		ps.setString(3, user.getPassword());
	}
}
```

```java
public void add(User user) throws SQLException {
	StatementStrategy st = new AddStatement(user); 
	jdbcContextWithStatementStrategy(st);
}
```

지금까지 해옹 작업만으로도 많은 문제점을 해결하고 코드도 깔끔하게 만들긴 했지만， 

먼저 DAO 메소드마다 새로운  StatementStrategy 구현 클래스를 만들어야 한다는 점과

 DAO 메소드에서 StatementStrategy 에 전달할 User와 같은 부가적인 정보가 있는 경우 이를 위해 오브젝트를 전달받는 생성자  와 이를 저장해둘 인스턴스 변수를 번거롭게 만들어야 한다는 두가지 문제점이 있다.

이 두 가지 문제를 해결할 수 있는 방법을 생각해보자.

### 로컬 클래스

> StatementStrategy 전략 클래스를 매번 독립된 파일로 만들지 말고 UserDao 클래스 안에 내부 클래스로 정의해버  리는 것이다.

```java
public void add(User user) throws SQLException { 
	class AddStatement implements StatementStrategy { //내부에 선언한 로컬클래스
		User user; 
        
		public AddStatement(User user) {
			this.user = user; 
       	}
        
	public PreparedStatement makePreparedStatement(Connection c) 
		throws SQLException {
	PreparedStatement ps = 
		c.prepareStatement("insert into users(id, name , password) 
			values(?,?,?)") ; 
		ps.setString(l , user.getld()); 
		ps.setString(2, user.getName()); 
		ps.setString(3, user.getPassword()); 
                           
		return ps;
        }
     }
     StatementStrategy st = new AddStatement(user); 
	 jdbcContextWithStatementStrategy(st);
 }
```

```java
public void add(final User user) throws SQLException { 
	class AddStatement implements StatementStrategy { 
		public PreparedStatement makePreparedStatement(Connection c) 
			throws SQLException { 
		PreparedStatement ps = c.prepareStatement( 
			insert into users(id, name , password) values(7,7,7)"); 
		pS.set5tring(1 , user.getld());  
		pS.set5tring(2, user.getName());  
		pS.set5tring(3, user.getPassword());
		
		return ps;
		}
	}
		StatementStrategy st = new AddStatement(); 
        //피라미터로 user을 전달하지 않아도된다.
		jdbcContextWith5tatement5trategy(st); 
}
```

### 익명 내부클래스

> 한 가지 더 욕심을 내보자. AddStatement 클래스는 add() 메소드에서만 사용할 용도로  만들어졌다. 그렇다면 좀 더 간결하게 클래스 이름도 제거할 수 있다. AddStatement를 익명 내부 클래스로 만들어보자.

```java
StatementStrategy st = new StatementStrategy() { 
	public PreparedStatement makePreparedStatement(Connection c) 
	throws SQLException { 
		PreparedStatement ps = 
			c.prepareStatement(“insert into users(id, name , password) 
				values(? ,?,?)"); 
		pS .setString(1 , user.getld()); 
		ps.setString(2, user.getName()); 
		pS.setString(3, user.getPassword()); 
                               
		return ps;
	}
};
```

```java
public void add(final User user) throws SQLException { 
	jdbcContextWithStatementStrategy( 
			new StatementStrategy() { 
				public PreparedStatement makePreparedStatement(Connection c) 
    				throws SQLException { 
						PreparedStatement ps = 
							c. prepareStatement("insert into users(id, name , 
								password) values(?,?,?)’); 
						pS .setString(1 , user.getld()); 
						pS setString(ι user.getName()); 
						pS .setString(3, user.getPassword()); 
                    
						return ps;
                    
                }
            }
        }
     );
}
```

### JdbcContext의 분리

> jdbcContextWithStatementStrategy() 를 UserDao 클래스 밖으로 독립시켜서 모든 DAO가 사용할 수 있게 해보자.

```
public class JdbcContext {
	private DataSource dataSource;
    
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public void workWithStatementStrategy(StatementStrategy stmt) throws 
		SQLException { 
			Connection c = null; 
			PreparedStatement ps = null; 
		try {
        	c = this.dataSource .getConnection(); 
			ps = stmt.makePreparedStatement(c); 
			ps.executeUpdate(); 
		} catch (SQLException e) { 
			throw e; 
		}finally {
			if (ps != null) { try ( pS .close(); } catch (SQLException e) {} } 
			if (c != null) { try (c .close(); } catch (SQLException e) {} }
		}
	}
}
```

> UserDao가 분리된 JdbcContext를 DI 받아서 사용할 수 있게만든다.

```java
public class UserDao {
	private JdbcContext jdbcContext; 
	
	public void setJdbcContextOdbcContext jdbcContext) ( 
		this.jdbcContext = jdbcContext; 
	}
	
	public void add(final User user) throws SQLException ( 
		this.jdbcContext.workWithStatementStrategy( 
			 new StatementStrategy() { ... }
		 );
 	}
 	
	 public void deleteAll() throws SQLException ( 
		this.jdbcContext.workWithStatementStrategy( 
			new StatementStrategy() { . .. }
		);
}
```

새롭게 작성된 오브젝트 간의 의존관계를 살며보고 이를 스프링 설정에 적용해보자. 

 프링의 DI는 기본적으로 인터페이스를 사이에  두고 의존 클래스를 바꿔서 사용하도록 하는 게 목적이다. 하지만 이 경우 JdbcContext  는 그 자체로 독립적인 JDBC 컨텍 트를 제공해주는 서비스 오브젝트로서 의미가 있을 뿐이고 구현 방법이 바뀔 가능성은 없다. 

따라서 인터페이스를 구현하도록 만들지않았다

인터페이스를 사용해서 클래스를 자유롭게 변경할 수 있게 하지는 않았지만，  JdbcContext를 UserDao와 Di 구조로 만들어야 할 이유를 생각해보자.

- . JdbcContext는 그 자체로 변경되는 상태정보를 갖고 있지 않다.  읽기 전용이므로 싱글톤이 되는데 아무런 문제가 없다 . 
-  JdbcContext 가 Di 를 통해 다른 빈에 의존하고 있기 때문이다. 이 두번째 이유가 중요하다.  JdbcContext는 dataSource 프로퍼 티를 통해 DataSource 오브젝트를  주입받도록 되어있다. DI 를 위해서는 주입되는 오브젝트와 주입받는 오브젝트 양쪽 모  두 스프링 빈으로 등록돼야 한다.

> tip ) 단，이런 클래스를 바로 사용하는 코드 구성을 DI에 적용히는 것은 가장 마지막 단계  에서 고려해볼 사항임을 잊지 말자.

### 코드를 이용하는 수동 DI

JdbcContext를 스프링의 빈으로 등록해서 UserDao에 DI 히는 대신 사용할 수 있는 방법이 있다. UserDao 내부에서 직접 DI를 적용하는 방법이다.

이 방법을 쓰려면 JdbcContext를 싱글톤으로 만들려는것은 포기해야한다. 물론 DAO 메소드가 호출 될때마다 JdbcContext 오브젝트를 새로 만드는 방법을 사용해야한다는것은 아니다. 조금 타협을 해서 DAO 마다 하나의 JdbcContext의 오브젝트를 갖고 있게하는 것이다.

JdbcContext를 스프링 빈으로 등록하지 않았으므로 다른 누군가가 JdbcContext 의  생성과 초기화를 책임져야 한다.. JdbcContext 의 제어권은 UserDao가 갖는 것이 적당하다.

> JdbcContext 에 대한 제어  권을 갖고 생성과 관리를 담당하는 UserDao에게 DI까지 맡긴다.
>
> JdbcContext 에 주입해줄 의존 오브젝트인 DataSource는 UserDao가 대신 DI 받도록 하면 된다.
>
> UserDao는 JdbcContext 오브젝트를 만들면서 DI 받은 DataSource 오브젝트  를 JdbcContext 의 수정자 메소드로 주입해준다.

```java
public class UserDao {

private JdbcContext jdbcContext;

pblic void setDataSource(DataSource dataSource){
	this.jdbcContext = new JdbcContext();
	
	this.jdbcContext.setDataSource(dataSource);
	
	this.dataSource = dataSource;
}
```

이 방법의 장점은 굳이 인터페이스를 두지 않아도 될 만큼 긴밀한 관계를 갖는 DAO  클래스와 JdbcContext를 어색하게 따로 빈으로 분리하지 않고 내부에서 직접 만들어  시용하면서도 다른 오브젝트에 대한 DI를 적용할 수 있다.

### 템플릿과 콜백

>  바뀌지 않는 일정한 때턴을 갖는  작업 흐름이 존재하고 그중 일부분만 자주 바꿔서 사용해야 히는 경우에 적합한 구조 다. 전략 패턴의 기본 구조에 익명 내부 클래스를 활용한 방을 스프링에서는 **템플릿 / 콜백 패턴**이라고 부른다.

댐플릿은 고정된 작업 흐름을 가진 묘드를 재사용한다는 의미에서 붙인 이름이다. 콜백은 템플릿 안에서 호출되는 것을 목적으로 만들어진 오브젝트를 말한다.

콜백은 일반적으로 하나의 메소드를 가진 인터페이스를 구현한 익명 내부 클래스로 만들어진다고 보면 된다.

콜백 인터페이스의 메소드에는 보통 파라미터가 있다. 이 파라미터는 템플릿의 작업 흐름 중에 만들어지는 컨텍스트 정보를 전달받을 때 사용된다. 

```java
public void deleteAll() throws SQLException ( 
	executeSql(‘ delete from users"); //변하는 sql문장
}
```

```java
private void executeSQl(final String Query) throws SQLException {
	this.jdbcContext.workWithStatementStrategy{
		new StatementStrategy() {
			public PreparedStatement makePreparedStatement(Connection c) 
						throws SQLException { 
					return c.prepareStatement(Query);
					//변하지 않는 콜백 클래스 정의와 오브젝트 생성 
			}
		}
	);
}
```

이렇게 재사용 가능한 콜백을 담고 있는 메소드라면 DAO가 공유할 수 있는 템플릿 클래스 안으로 옮겨도 된다. 

엄밀히 말해서 템플릿은 JdbcContext 클래스가 아니라  workWithStatementStrategy() 메소드이므로 JdbcContext 클래스로 콜백 생성과 댐플  릿 호출이 담긴 executeSql () 메소드를 옮긴다고 해도 문제 될 것은 없다.

```java
public class JdbcContext { 
public void executeSql(final String query) throws SQLException {
	workWithStatementStrategy{
		new StatementStrategy() {
			public PreparedStatement makePreparedStatement(Connection c) 
				throws SQLException {
			return c.prepareStatement(query);
			}
		}
	);
}
```

> UserDao의 메소드에서도 리스트  3-29와 같이 jdbcContext를 통해 executeSql() 메소드를 호출하도록 수정

```java
public void deleteAll() throws SQLException { 
	this.jdbcContext.executeSql("delete from users");
}
```

일반적으로는 성격이 다른 묘드들은 가능한 한 분리하는 편이 낫지만， 이 경우는 반대다. 하나의 목적을 위해 서로 긴밀하게 연관되어 동작하는 웅집력이 강한 코드들이기  때문에 한 군데 모여 있는 게 유리하다.

### 테스트와 try/catch/finally

설명보다는 리팩토링 위주여서 , 프로젝트로 작성하였습니다.

[ 링크에 프로젝트에서 참고해주세요. ]()

( 현재 머지하지 않은상태라 링크를 비워두웠습니다 . chapter3에서 Test 프로젝트를 확인해주세요 )

리팩토링 순서는 상위폴더에 Calculator → BufferedReader 패키지 → Lineread 패키지  → Generics 패키지 순으로 참고해주시면됩니다 : ) 

### 스프링의 JdbcTamplate

스프링은 JDBC를 이용히는 DAO에서  사용할 수 있도록 준비된 다양한 템플릿과 콜백을 제공한다.

```java
public class UserDao {

	private JdbcTemplate jdbcTemplate; 
	
		public void setDataSource(DataSource dataSource) {
			this.jdbcTemplate = new JdbcTemplate(dataSource); 
			
			this.dataSource = dataSource;
}
```

```java
public void deleteAll() { 
	this.jdbcTemplate.update(
		new PreparedStatementCreator() ( 
			public PreparedStatement createPreparedStatement(Connection con) 
					throws SQLException ( 
				return con.prepareStatement( "delete from users");
			}
		}
	);
}
```

### JdbcTemplate 메소드 정리

| 메소드           | 설명                                                         |
| ---------------- | ------------------------------------------------------------ |
| UPDATE()         | 치환자를 가진 SQL을 만든 후 함께 제공하는 파라미터를 활용하면 된다. |
| QueryForlnt()    | SQL 쿼리를 실행하고 **정수의 결과 값(Integer 타입)**을 가져올 때 사용하는 메소드 |
| QueryForObject() | SQL의 DML 중 SELECT를 실행했을 때 **하나의 객체(Object) 결과 값**이 나올 때 사용하는 메소드 |
| query()          | queryForInt()가 하나의 결과 값을 위한 메소드인 반면, 많은 결과 값(로우 값)을 처리 할 수 있는 메소드 |

> update 예시

```java
this.jdbcTemplate.update("insert into users(id, name , password) values(? ,?,?)", 
user.getld() , user .getName() , user .getPassword());
```

> QueryForlnt() 예시

```java
public int getCount() {
	return this.jdbcTemplate .queryForlnt("select count(*) from users'’);
}
```

> QueryForObject() 예시

```java
public User get(String id ) { 
	return this.jdbcTemplate.QueryForObject("select * from users where id = ?" , 
		new Object[] {id}, //sql에 바인딩할 파라미터값, 가변인자대신 배열을 사용
		new RowMapper<User>(){
			public User mapRow(ResultSet rs, int rowNum) 
				throws SQLException {
				User user = new User(); 
				user.setld(rs.getString("id")); 
				user.setName(rs.getString("name'’ )); 
				user.setPassword(rs.getString("password")); 
				return user;
		} //resultset한 로우값의 결과를 오브젝트에 매핑해주는 RowMapper 콜백 
	});
}
```

> query()예시

```java
public List<User> getAll() {
	return this.jdbcTemplate.Query("select * from users order by id" , 
		new RowMapper<User>() {
			public User mapRow(ResultSet rs, int rowNum) 
					throws SQLException ( 
				User user = new User(); 
				user.setld(rs.getString("id")); 
				user .setName(rs .getString("name")); 
				user.setPassword(rs.getString("password")); 
				return user; 
			}
		});
}
```

### 재사용 가능하도록 독립시킨 RowMapper

```java
public class UserDao{
	private RowMapper<User> userMapper = 
		new RowMapper<User>() {
			public User mapRow(ResultSet rs, int rowNum) 
					throws SQLException ( 
				User user = new User(); 
				user.setld(rs.getString("id")); 
				user .setName(rs .getString("name")); 
				user.setPassword(rs.getString("password")); 
				return user; 
			}
		}
}
```

```java
public User get(String id) ( 
	return this.jdbcTemplate.QueryForOb ject(“select * frαn users where id = 7" , 
		new Object[] {id} , this,userMapper);
}
public List<User> getAll() ( 
    return this.jdbcTemplate.Query( elect * from users order by id" , 
		this, userMapper);
}
```

