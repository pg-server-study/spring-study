

# 3장 템플릿

**템플릿이란 ?** 바뀌는 성질이 다른 코드 중에서 **변경이 거의 일어나지 않으며 일정한 패턴으로 유지되는 특성을 가진 부분을 자유롭게 변경이 되는 성질을 가진 부분으로부터 독립 **시켜서 효과적으로 활용 할 수 있도록 하는 방법.

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

개방 패쇄 원칙OCP을 잘 지키는 이면서도 템플릿 메소드 패턴보다 유연하고 확  장성이 뛰어난 것이， 오브젝트를 아예 둘로 분리하고 클래스 레벨에서는 인터페이스  를 통해서만 의존하도록 만드는 전략 패턴이다.

219p
