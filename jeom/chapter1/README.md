

#  오브젝트와 의존 관계

>일단 사용자 정보를 jdbc api를 통해 db에 저장하고 조회할수 있는 dao를 만들어보자.

> 예시코드 ( User clas 생략 )

```java
public class UserDao{
	public void add(User user) throws ClassNotFoundExcetion,SQLException {
        // 사용자 정보를 생성하는 메소드 
		Class.forName("com.mysql.jdbc.Driver");
        Connciton c = DriverManger.getConnection(
            	"jdbc:mysq://localhost/springbook","spring","book";
        	);
        PreparedStatement ps = c.prepareStatement(
            	"insert into users(id, name , password) values(?,?,?)";
        );
        
        ps.setString(1,user.getId());
        ps.setString(1,user.getName());
        ps.setString(1,user.getPassword());
        
        ps.excuteUpdate();
        
        ps.close();
        c.close();
	}
    
    public User get(String id) throws ClassNotFoundExcetion,SQLException {
        // 사용자 정보를 가져오는 메소드 
		Class.forName("com.mysql.jdbc.Driver");
        Connciton c = DriverManger.getConnection(
            	"jdbc:mysq://localhost/springbook","spring","book";
        	);
        PreparedStatement ps = c.prepareStatement(
            	"insert into users(id, name , password) values(?,?,?)";
        );
        
       ps.setStirng(1,id);
       
       ResultSet re = ps.executeQuery();
       rs.next();
       User user = new User();
       user.setId(rs.getString("id"));
       user.setName(rs.getString("name"));
       user.setPassword(rs.getString("password"));
        
       rs.close();
       ps.close();
       c.close();
        
       return user;
	}
}
```

>기능에는 전혀문제가 없겠지만 유지 , 보수 , 확장하기 어려운 **초난감 Dao가 완성 되었다 . **
>
>이제 초난감한 Dao를 개선해보자 .

### **관심사 분리 **

> 현재 초난감 dao 에서 기능 변경이 있을경우 **어떻게 필요한 최소한 작업을 최소화 하고 , 변경이 문제를 일으키지 않게** 할수 있을까 ?
>
> 위에 초난감 Dao에서 db 암호를 바꾸려면 ? Dao 클래스가 많을수록 그 많은 클래스들을 전부 수정해야한다.
>
> 프로그래밍 기초 개념중에 **관심사의 분리 **라는게 있다 . 이를 객체 지향에 적용해 보면 ,
>
> 관심이 같은 것끼리는 하나의 객체안으로 또는 친한 객체로 모이게 하고 ,
>
> 관심이 다른것 끼리는 가능한 따로 떨어져서로 영향을 주지 않도록 분리하는것이라 생각 해볼수 있다 .

***

> **UserDao의 관심사항 **

- db연결을 위한 커넥션을 어떻게 가져올지
- 사용자 등록을 위해 db에 보낼 sql 문장을 담을 Statement를 만들고 실행하는것
- 작업이 끝나면 리소스인 Statemane 와  Connection 오브젝트를 닫아주는것.

> **가장 문제가 되는건 db 커넥션을 가져오는 부분이다** . 앞으로 여러개 , 수백개의 dao 클래스를 만들면 db커넥션을 가져오는 코드도 수백개가 되어 버릴것이다 . 

***

> 중복 코드의 메소드 추출 

```java
public void add(User user) throws ClassNotFoundExctption,SPLExcetion{
	Connection c = getConnection();
	... //생략
}
public User get(String id) throws ClassNotFoundExctption,SPLExcetion{
	Connection c = getConnection();
	... //생략
}
private Connection getConnection() throws ClassNotFoundExctption,SPLExcetion {
   Class.forName("com.mysql.jdbc.Driver");
        Connciton c = DriverManger.getConnection(
            	"jdbc:mysq://localhost/springbook","spring","book";
        	);
    return c;
}
```

> 나중에 db연결 부분에 변경이 일어 났을 경우 , getConnection 메소드만 수정해 주면 된다 .

***

### **클래스의 분리**

> 슈퍼 울트라 캡숑  dao가 되어 버려서 구매하고 싶다는 회사에 납품을 하려한다 .
>
> 하지만 n사와 d사와 각기 다른 종류의 db를 사용하고 db 커넥션을 가져오는데 독자적인 방법으로 적용 하고 싶어 한다는 문제가 생겼다 . 더 큰문제는 db커넥션을 가져오는 방법이 종종 변경 될 가능성이 있다는점 !
>
> 슈퍼 울트라 캡숑 UserDao 소스코드를 공개하고 알아서 수정하라 할 수 있지만,
>
> 어떻게 하면 **울트라 캡숑 기술을 공개하고 싶지않으면서 고객스스로 원하는 db 커네션 생성 방식을 사용하게 할수있을까**?

***

> **상속을 통한 확장**
>
> 기존 UserDao 코드를 한단계더 분리한다 . 
>
> UserDao에서 구현 코드를 제거하고 getConnection()을 추상메소드로 만든다.
>
> -> 따라서 add() , get() 메소드에서 getConnection() 을 호출하는 코드는 그대로 유지할수 있다 .

> 수정코드 

```java
public avstract class UserDao{
    public void add(User user) throws ClassNotFoundException, SQLException{
        Connection c = getConnection();
        ... //생략
    }
     public User get(String id) throws ClassNotFoundException, SQLException{
        Connection c = getConnection();
        ... //생략
    }
    pubilc abstract Connection getConnection() throws ClassNotFoundException, SQLException;
    //구현코드는 제거 되고 추상메서드로 바뀌었다 , 메소드의 구현은 서브클래스가 담당 
}
```

```java
public class NuserDao extends UserDao{
	public Connection getConnection()  throws ClassNotFoundException, SQLException{
		//n사 db 생성코드
	}
}

public class DuserDao extends UserDao{
	public Connection getConnection()  throws ClassNotFoundException, SQLException{
		//d사 db 생성코드
	}
}
```

> **상속을 통해 관심을 분리하고 변신이 가능하도록 확장성도 줬지만 문제점이 있다 **

- 자바는 다중 상속을 허용하지 않기때문에 나중에 다른이유로 UserDao에 상속을 하기 어렵다 .
- 서브클래스는 슈퍼클래스의 기능을 직접 사용 할수 있기 때문에 , 슈퍼 클래스 내부의 변경이 있을경우 모든 서브클래스를 함께 수정하거나 다시 개발해야할수도 있음
- 확장 기능인 db 커넥션을생성하는 코드를 다른 Dao 클래스에 적용할수 없다 . ( 다른 Dao 클래스들이 계속만들어 진다면 상속을 통해 만들어진  getConnection() 의 구현 코드가 매 Dao 클래스마다 중복됨  )

***

> **아예 화끈하게 독립적인 클래스로 분리해버리자 ! **
>
> 새로운 SimpleConnectionMaker 이라는 클래스를 만들고 db 생성기능을 그 안에 넣는다 .
>
> 그리고 UserD매 는 new 키워드를 사용해  SimpleConnectionMaker  클래스의 오브젝트를 만들어 두고 ,
>
> 이를 add , get 메소드에서 사용하면 된다 . 각 메소드에서 SimpleConnectionMaker 의 오브젝트를 만들 수도 있지만
>
> 그보다는 한번만 SimpleConnectionMaker  오브젝트를 만들어서 저장해두고 , 이를 계속 사용하는 편이 낫다 .

> 수정 코드 ( db생성 기능을 독립시킨 simpleConnectionMaker 클래스는 생략 . )

```java
public class UserDao{
	private SimpleConnectionMaker  simpleConnectionMaker;
	
	public UserDao(){
		simpleConnectionMaker = new SimpleConnectionMaker ();
	}
    
    public void add(User user) throws ClassNotFoundException, SQLException{
        Connection c = simpleConnectionMaker.makeNewConnection();
        ... //생략
    }
    
    public User add(String id) throws ClassNotFoundException, SQLException{
        Connection c = simpleConnectionMaker.makeNewConnection();
        ... //생략
    }
}
```

> **화끈하게 수정을 했지만 상속과는 다른 문제가 발생했다 **

- 상속을 통해 db 커넥션 기능을 확장해서 사용하게 했던게 다시 불가능해 졌다 .
- 코드의 수정없이 db 커넥션 생성 기능을 변경할 방법이 없다 .

***

### **인터페이스의 도입 **

>클래스를 분리하면서도 이런 문제를 해결하기 위해 두개의 클래스가 긴밀하게 연결되어 있지 않도록 중간에 추상적인 느슨한 
>
>연결고리를 만들어 주는것이다 . 
>
>인터페이스를 사용해 초기 코드에서 클래스를 분리 ,
>
>자신을 구현한 클래스에 대한 구체적인 정보는 감춰버림
>
>-> 오브젝트를 만들면 구체적인 클래스 하나를 선택해야 겠지만 , 
>
>인터페이스로 추상화 해놓은 최소한의 통로를 통해 접근하는 쪽에서는 
>
>**사용할 클래스가 무엇인지 몰라도된다 **.
>
>**인터페이스를 통해 접근하게 하면 실제 구현 클래스를 바꾸어도 신경쓸 일이없다.**

> 수정된 코드  

```java
public class DConnectionMaker implements ConnectionMaker{
	public Conneciton makeConnection() throws ClassNotFoundException , SQLException{
			// Connetction 을 생성하는 코드 
	}
}
```

```java
public class UserDao{
	private ConnectionMaker connecitonMaker;
    //인터페이스를 통해 오브젝트에 접근 하므로 구체적인 프로젝트 정보를 알 필요가 없다 .
	
	public UserDao(){
		connectionMaker = new DConnectionMaker();
        // 근데 여기는 클래스 이름이 나오네 ;
	}
	
	public void add(User user) throws lassNotFoundException, SQLException{
		Connection c = connectionMaker.makeConnection();
		...
        //인터에이스에 정의 된 메소드를 사용하므로 클래스가 바뀐다고 해도 이름이 변경될 걱정 x 
	}
}
```

>db 커넥션을 제공하는 클래스에 대한 구체적인 정보는 모두 제거가 가능했지만 
>
>**여전히 초기에 한번 어떤 클래스의 오브젝트를 사용할지 결정하는 생성자의 코드는 제거되지 않고 남아있다 ! **

>new DConnectionMaker() 라는 코드는 짧고 간단하지만 그자체로 충분히 독립적인 관심사를 담고 있다 .
>
>바로 UserDao가 어떤 ConnectionMaker 구현클래스의 오브젝트를 이용하게 할지 결정하는 것이다 .
>
>간단히 말하자면 **UserDao와 UserDao가 사용할 ConnectionMaker 의 특정구현 클래스 사이의 관계를 설정**해주는것에 대한 관심이다. 
>
>**이관심사를 담은 코드를 UserDao에서 분리하지 않으면 UserDao는 결코 독립적으로 확장 가능한 클래스가 될수 없다.**

***

> **그럼 어떻게 분리하죠 ?**

> UserDao를 사용하는 클라이언트가 적어도 하나는 존재할 것이다 . 
>
> 왜 뜬금없이 클라이언트 오브젝트를( 서비스를 제공하는 쪽 클라이언트 )  를 끄집어 내냐면
>
> **UserDao의 클라이언트 오브젝트가 바로 제3의 관심사항인 UserDao와 ConnectionMaker의 구현 클래스의 관계를 결정해주는 기능을 분리해서 두기 적절한곳이기 때문이다 !**

***

> 클라이언트에서 **UserDao 를 사용하기 전에 먼저 UserDao가 어떤 ConnectionMaker의 구현 클래스를 사용 할지 결정 하도록 만들어보자.**
>
> 즉 UserDao 오브젝트와 특정 클래스로 부터 만들어진 ConnectionMaker 오브젝트 사이에 관계를 설정해주는것이다 .
>
> 사실 클래스 사이의 관계를 설정해주는것은아니다.
>
> 클래스 사이에 관계가 만들어 진다는 것은 한 클래스가 인터페이스 없이 다른 클래스를 직접 사용 한다는 뜻이다 . 
>
> 따라서 클래스가 아니라 **오브젝트와 오브젝트 사이의 관계를 설정 **해줘야한다.
>
> `conncetionMaker = new DConnectionMaker();` 은 DConnectionMaker의 오브젝트의 레퍼런스를 UserDao의 
>
> connectionMaker 변수에 넣어서 사용하게 함으로써 이 두개의 오브젝트가 사용 이라는 관계를 맺게 해준다 .
>
> 오브젝트 사이의 관계가 만들어지려면 일단 만들어진 오브젝트가 있어야하는데 , 이처럼 직접 생성자를 호출해서 직접 오브젝트를 
>
> 만드는 방법도 있지만 외부에서 만들어 준것을 가져오는 방법도 있다 . UserDao도 동작하려면 하나의 오브젝트가 만들어 질것이다.
>
> **UserDao 오브젝트가 다른 오브젝트와 관계를 맺으려면 관계를 맺을 오브젝트가 있어야하는데 이 오브젝트를 꼭 UserDao코드 내에서 만들 필요는 없다 . 오브젝트는 얼마든지 메소드 파라미터 등을 이용해 전달할수 있으니 외부에서 만든걸 가져올 수 도있다 .**
>
> 외부에서 만든 오브젝트를 전달 받으려면 메소드 파라미터나 생성자 파라미터를 이용 하면된다 . 이때 파라미터의 타입을 전달받을 오브젝트의 인터페이스로 선언해뒀다고 해보자 . 이런경우 파라미터로 전달되는 오브젝트이 클래스는 해당 인터페이스를 구현 하기만 했다면 어떤 것이든지 상관없다. 단지 해당 인터페이스 타입의 오브젝트라면 파라미터로 전달 가능하고 , 파라미터로 제공받은 오브젝트는 인터페이스에 정의된 메소드만 이용 한다면 그 오브젝트가 어떤 클래스로부터 만들어 졌는지 신경 쓰지 않아도된다 .

> UserDao의 모든 코드는 ConnectionMaker 인터페이스 외에는 어떤 클래스와도 관계를 가져서는 안되게 해야한다 .



![image-20211004002639858](C:\Users\mijeo\AppData\Roaming\Typora\typora-user-images\image-20211004002639858.png)



> 1-4 와같은 의존관계가 만들어 져야 UserDao의 수정없이 db 커넥션 구현 클래스를 변경 할수 있다 .

![image-20211004002728187](C:\Users\mijeo\AppData\Roaming\Typora\typora-user-images\image-20211004002728187.png)

> 물론 UserDao 오브젝트가 동작하려면 특정 클래스의 오브젝트와 관계를 맺어야한다 db 커넥션을 제공하는 기능을 가진 
>
> 오브젝트를 사용하기는 해야하니 말이다 . 결국 특정 클래스의 오브젝트와 관계를 맺게된다 .
>
> 하지만 클래스사이에 관계가 만들어진것은 아니고 ,  **단지 오브젝트 사이에 다이내믹한 관계가 만들어지는것이다 .**

> UserDao 오브젝트가 DConnectionManager 오브젝트를 사용하게 하려면 두 클래스의 오브젝트 사이에 
>
> 의존 관계라고 불리는 관계를 맺어주면된다 .

***

> **의존 관계를 어떻게 맺어 줄까 ? **

![image-20211004003202680](C:\Users\mijeo\AppData\Roaming\Typora\typora-user-images\image-20211004003202680.png)

> 위와 같은 런타임 오브젝트 관계를 갖는 구조로 만들어 주는게 클라이 언트의 책임이다 .
>
> 클라이언트는 자기가 UserDao를 사용 해야할 입장이기 때문에 UserDao의 세부전략이라고 볼수 있는 ConnectionMaker 의 구현 클래스를 선택하고 , 선택한 클래스의 오브젝트를 생성해서 UserDao와 연결해 줄수 있다 .

***

### **클라이언트에게 떠넘기자 ! **

> UserDaoTest라는 클래스를 하나만들고 UserDao의 생성자를 수정해서 클라이언트가 미리 만들어둔 ConnectionMaker 의 오브젝트를 전달 받을수 있도록 파라미터를 하나 추가한다 .
>
> 클라이언트와 같은 제 3의 오브젝트가 UserDao 오브젝트가 사용할 ConnectionMaker 오브젝트를 전달해주도록 만든것이다 .

```java
public UserDao (ConnectionMaker connectionMaker){
	this.connectionMaker = connectionMaker;
}
```

> **DConnectionMaker을 생성하는 코드는 어디에 ?**
>
> DConnectionMaker를 생성하는 코드는 UserDao 와 특정 ConnectionMaker 구현 클래스의 으브젝트간 관계를 맺는 책임을 담당 하는 코드 였는데 그것을 UserDao의 클라이언트에 넘겨 버렸기 때문이다 .

> 클라이언트 코드 

```java
public class UserDaoTser {
	public static void main(String [] args) throws ClassNotFoundException, SQLException{
		ConnectionMaker connectionMeker = new DConnectionMaker();
		//UserDao가 사용할 ConnectionMaker 구현 클래스를 결정하고 오브젝트를 생성한다 .
		UserDao dao = new UserDao(connectionMaker);
        ... // 생략
	}
}
```



![image-20211004004430194](C:\Users\mijeo\AppData\Roaming\Typora\typora-user-images\image-20211004004430194.png)

> 서로 영향을 주지 않으면서도 필요에따라 자유롭게 확장 할수 있는 구조가 되었다 .

***

> **원칙과 패턴**

- 개방 폐쇄 원칙 ( 클래스나 모듈은 확장에는 열려 있어야하고 변경에는 닫혀있어야한다 . )
- 높은 응집도 ( 변화가 일어날 때 해당 모듈에서 변하는 부분이 크다. )

- 낮은 결합도 ( 결합도 - 하나의 오브젝트가 변경이 일어 날 때에 관계를 맺고 있는 다른 오브젝트에게 변화를 요구하는 정도 )

> 전략패턴
>
> 개선한 구조를 디자인 패턴의 시작으로 보면 전략패턴에 해당한 다고  볼수 있다 .
>
> 전략 패턴은 **필요에 따라 변경이 필요한 알고리즘은 인터페이스를 통해 외부로 분리시키고 , 이를 구현한 구체적인 알고리즘 클래스를 필요에 따라 바꿔서 사용 할수 있게하는 디자인 패턴이다 ** 

***

# 제어의 역전 ( IOC )

> **지금 까지는 초난감 Dao를 깔끔한 구조로 리팩토링 하는 작업을 수행했다 .**
>
> 얼렁 뚱땅 넘어갔지만 현재 UserDaoTest가 어떤 ConnectionMaker 구현 클래스를 사용할지 떠맡고 있다 .
>
> UserDao 와 ConnectionMaker구현 클래스의 오브젝트를 만드는 것과 , 
>
> 그렇게 만들어진 두개의 오브젝트를 연결되어 사용 될수 있도록 관계를 맺어 주는것을 분리하자 ! 

### 팩토리 

> 분리시킬 기능을 담당할 클래스를 하나 만들어 보겠다 . 이클래스의 역할은 객체의 생성 방법을 결정하고 그렇게 만들어진 
>
> 오브젝트를 돌려주는 것인데 , **이런 일을 하는 오브젝트를 흔히 팩토리 라고 부른다. ** 
>
> **단지 오브젝트를 생성하는 쪽과 생성된 오브젝트를 사용하는 쪽의 역할과 책임을 깔끔하게 분리하려는 목적으로 사용하는 것이다. **

> 팩토리 역할을 맡을 클래스를 DaoFactory라고 하자 , 
>
> 그리고 UserDaoTest에 담겨있던 UserDao, ConnectionMaker관련 생성 작업을
>
> DaoFactory로 옮기고 ,  UserDaoTest서는 DaoFactory에 요청에서 미리 만들어진 UserDao 오브젝트를 가져와 사용하게 만든다 .

```java
public class DaoFactory{
	public UserDao userDao(){
		ConnectionMaker connectionMaker = new DConnectionMaker();
		UserDao userDao = new UserDao(connectionMaker);
        //userdao 타입의 오브젝트를 어떻게 만들고 준비 시킬지 결정한다 .
		return userDao;
	}
}
```

```java
public class UserDaoTest{
	public static void main(String []args) throws ClassNotFoundException,SQLException{
		UserDao dao = new DaoFactory().userDao();
	}
}
```

> userDao 메소드를 호출하면 DConnectionMaker를 사용해 db 커넥션을 가져오도록 이미 설정된 UserDao 오브젝트를 돌려준다.
>
> UserDaoTest 는 이제 UserDao가 어떻게 만들어지는지 초기화 되는지에 신경쓰지않을수 있다 .

***

### 제어권의 이전을 통한 제어관계 역전

> 이제 제어의 역전이라는 개념에 대해 알아보자. 제어의 역전이라는 건，
>
> 간단히 프로그램의 제어 흐름 구조가 뒤바뀌는 것이라고 설명할 수 있다. 
>
> 일반적으로 프로그램의 흐름은 main() 메소드와 같이 프로그램이 시작되는 지점  에서 
>
> 다음에 시용할 오브젝트를 결정하고， 결정한 오브젝트를 생성하고， 
>
> 만들어진 오  브젝트에 있는 메소드를 호출하고 그 오브젝트 메소드 안에서 다음에 사용할 것을 결  정하고 
>
> 호출히는 식의 작업이 반복된다. 
>
>  초기 UserDao를 보면 테스트용 main() 메소드는 UserDao 클래스의 오브젝트를 직  접 생성하고， 
>
> 만들어진 오브젝트의 메소드를 시용한다
>
>  UserDao 또한 자신이 사용할  ConnectionMaker의 구현 클래스( 예를 들면 DConnectionMaker )를 자신이 결정하고，
>
>  그 오브젝트를 펼요한 시점에서 생성해두고 각 메소드에서 이를 시용한다.
>
> **제어의 역전이란 이런 제어 흐름의 개념을 꺼꾸로 뒤집는 것이다.**

***

> **제어의 역전에서는 오브젝트가 자신이 사용할 오브젝트를 스스로 선택하지 않는다. 당연히 생성하지도  않는다. **
>
> **또 자신도 어떻게 만들어지고 어디서 사용되는지를 알 수 없다. **
>
> **모든 제어 권한  을 자신이 아닌 다른 대상에게 위임하기 때문이다.**
>
> 프로그햄의 시작을 담당하는 main()  과 같은 엔트리 포인트를 제외하면
>
> **모든 오브젝트는 이렇게 위임받은 제어 권한을 갖는  특별한 오브젝트에 의해 결정되고 만들어진다.**

> TiP > 라이브러리 - 동작하는 중에 필요한 기능이 있을때 능동적으로 라이브러리를 사용한다 . ( 애플리케이션 흐름 직접제어 )
>
> ​	  > 프레임워크 - 라이브러리오 다르게 거꾸로 애플리케이션 코드가 프레임워크에 의해 사용된다 .
>
> ​		( 보통 프레임워크 위에 개발한 클래스를 등록해 두고 , 프레임워크가 흐름을 주도하는 중에 
>
> ​		개발자가 만든 애플리 케이션 코드를 사용하도록 만드는 방식 )

> 우리가만든 UserDao 와 DaoFactory 에도 제어의 역전이 적용되어 있다.

-  원래  ConnectionMaker 의 구현 클래스를 결정하고 오브젝트를 만드는 제어권은 UserDao에게  있었다. 그런데 지금은 DaoFactory 에게 있다.
-  UserDaoTest는 DaoFactory가 만들고 초기화해서 자신에게 사용하도록 공급해주는 ConnectionMaker를 사용할 수밖에 없다.
-  더욱이 UserDao와 ConnectionMaker  의 구현체를 생성히는 책임도 DaoFactory 가 맡고 있다.

> 자연스럽게 관심을 분리하고 책임을 나누고 유연하게 확장 가능한 구조로 만들기 위해 DaoFactory를 도입했던 과정이
>
>  바로 IoC를 적용하는작업이었다고볼수있다. 
>
>  우리는 대표적인 IoC 프레입워크라고 불리는 스프링 없이도 IoC 개념을 이미 적용한 셈이다.
>
> **IoC는 기본적으로 프레임워크만의 기술도 아니고 프레임워크가 꼭 필요한 개념도 아니다.** 
>
> 단순하게 생각하면 디자인 패턴에서도 발견할 수 있는 것처럼 상당히 폭넓게 사용되는 **프로그래밍 모델**이다.

***

# 스프링의 IOC

> 용어

***

- 빈 - 빈 또는 빈 오브젝트는 스프링이 IoC 방식으로 관리하는 오브젝트라는 뜻이다.

   ( 만들어지는 모든 오브젝트가 빈이 아니라 스프링이 직접 그 생성과 제어를 담당하는 오브젝트만을 빈이라고 부른다. )

  

- 빈 팩토리 -  빈을 등록하고,생성하고，조회하고 돌려주고，그 외에 부가적인 빈을 관리히는 기능을 담당한다. 

  보통은 이  빈 팩토리를 바로 사용하지 않고 이를 확장한 애플리케이션 컨텍스트를 이용한다. 

  BeanFactory 라고 붙여쓰면 빈 팩토리가 구현하고 있는 가장 기본적인 인터페이스의  이름이 된다. 

  이 인터페이스에 getBean() 과 같은 메소드가 정의되어 있다. 

  

- 애플리케이션 컨텍스트 - 빈 팩토리를 확장한 IoC 컨테이너다. 빈을 등록하고 관리하는 기본적인 기능은 빈  팩토리와 동일하다.

  스프링이 제공하는 각종 부가 서비스를 추가로 제공한다. 

  **애플리케이션 컨텍스트는 설정정보를 참고해서 빈의 생성 , 관계설정등의 제어 작업을 총괄한다. **

  빈 팩토리라고 부를 때는 주로 빈의 생성과 제어의 관점에서 이야기하는 것이고， 

  애플리케이션 컨텍스트라고 할 때는 스프링이 제공히는 애플리케이션 지원 기능을  모두 포함해서 이야기하는 것이라고 보면 된다.

  스프링에서는 애플리케이션 컨텍스트라는 용어를 빈 팩토리보다 더 많이 사용한다. 

  ApplicationContext 라고 적으면 애플리케이션 컨텍트스가 구현해야 하는 기본 인터페이스를 가리키는 것이기도 하다.  

  ApplicationContext는 BeanFactory를 상속한다.

  

- 설정정보 설정 메타정보 -  애플리케이션 컨텍스트 또는 빈 팩토리가 IoC를 적용하기 위해 사용하는 메타정보를 말한다.

  실제로 스프링의 설정정보는 컨테이너에 어떤 기능을 셋팅 하거나  

  오브젝트와의존관계를 세팅하거나 조정하는 경우에도 사용하지만，

  그보다는 IoC 컨테이너에 의해 관리 되는 애플리케이션 오브젝트를 생성하고 구성할 때 사용된다.

***

>빈 팩토리 또는 애플리케이션 컨텍스트가 사용하는 설정정보를 만드는 방법은 여러가지가 있는데，
>
> 우리가 앞에서 만든 오브젝트 팩토리인 DaoFactory도 조금만 손을 보면  설정정보로 사용할 수 있다. 
>
>앞에서는 DaoFactory 자체가 설정정보까지 담고 있는 IoC  엔진이었는데
>
> 여기서는 자바 코드로 만든 애플리케이션 컨텍스트의 설정정보로 활용될  것이다.
>
>DaoFactory를 스프링의 빈 팩토리가 사용할 수 있는 본격적인 설정정보로 만들어보자.

```java
@Configuration // 애플리케이션 텍스트 또는 빈팩토리가 사용힐 설정정보라는표시 
public class DaoFactory { 
    
	@Bean	// 오브젝트 생성을 담당하는 ioC용 메소드리는 표시 
	public UserDao userDao() { 
		return new UserDao(connectionMaker());
    }
    
    @Bean
    public ConnectionMaker connectionMaker(){
        return new DConnectionMaker();
    }
}
```

>  이제 DaoFactory를 설정정보로 사용하는 애플리케이션 컨텍스트를 만들어보자.

```java
public class UserDaoTest ( 
	public static void main(String[) args) throws ClassNotFoundException, SQLException ( 
		ApplicationContext context = // 애플리케이션 컨텍스트는 ApplicationContext 타입의 오브젝트다.
				new AnnotationConfigApplicationContext(DaoFactory.class); 
   //@Configuration 이 붙은 자바코드를 설정정보로 시용하려면 AnnotationConfigApplicationContext를 이용하면 된다.
		UserDao dao = context.getBean("userDao" , UserDao.class)
        //getBean() 메소드는 ApplicationContext가 관리하는 오브젝트를 요청히는 메소드
   		... //생략
}
```

> 애플리케이션 컨텍스트를 만들 때 생성자 따라미터로 DaoFactory 클래스를 넣어  준다. 
>
> 이제 이렇게 준비된 ApplicationContext 의 getBean() 이라는 메소드를 이용해  UserDao의 오브젝트를 가져올 수 있다.
>
> **스프링을 적용하긴 했지만 사실 앞에서 만든 DaoFactory를 직접 사용한것과 기능적으로 다를 바가 없다 . **
>
> **오히려 더 번거로운 작업과 준비 코드가 필요해 졌다 .**
>
> **스프링을 사용해 ioc 를 적용 했다고 해서 별로 장점이 없지 않을까 하는 의문이 들 수도 있다 .**
>
> **물론 그렇지 않다. 스프링은 지금 우리가 구현했던 DaoFactory를 통해서는 얻을 수 없는 방대한 기능과 활용 방법을 제공해준다 .**
>
> **이런 특성에 대해서는 앞으로 계속 살펴볼 예정이다.**

***

### 애플리케이션 컨텍스트의 동작방식

> DaoFactory -> UserDao를 비롯한 DAO 오브젝트를 생성하고 DB 생성 오브젝트와  관계를 맺어주는 제한적인 역할
>
> 애플리케이션 컨텍스트 -> 애플리케이션에서 IoC를 적용해서 관리할 모든 오브젝트에 대한 생성과 관계설정을 담당.
>
> (  직접 오브젝트를 생성하고 관계를 맺어주는 코드가 없고， 그런 생성정보와 연관관계 정보를 별도의 설정정보를 통해 얻는다. )

***

> DaoFactory를 오브젝트 팩토리로 직접 사용했을 때와 비교해서 애플리케이션 컨텍스트를 사용했을 때 얻을 수 있는 장점

- **클라이언트는 구체적인 팩토리 클래스를 알필요가 없다.**

  (  loc를 적용한 오브젝트가 많아질 경우 필요한 오브젝트를 가져오려면 어떤 팩토리 클래스를 사용해야할지 알아야하고 , 

  필요할때마다 팩토리 오브젝트를 생성해야하는 번거로움이 있다 . 애플리 컨텍스트를 사용하면 오브젝트 팩토리가 아무리 많아져도 

  이를 알아야하거나 직접 사용할 필요가 없다 .)

- **애플리케이션 컨텍스트는 종합 loC 서비스를 제공해준다 .**

  (애플리케이션 컨텍스트의 역할은 단지 오브젝트 생성과 다른 오브젝트와의 관계설정만이 전부가 아니다. 

  오브젝트가 만들어지는 방식 시점과 전략을 다르게 가져갈  수도 있고， 

  이에 부가적으로 자동생성， 오브젝트에 대한 후처리， 정보의 조합， 설정 방식의 변화,

  인터셉팅 등 오브젝트를 효과적으로 활용할 수 있는 다양한 기능을  제공한다. 

  또， 빈이 사용할 수 있는 기반기술 서비스나 외부 시스템과의 연동 등을  컨테이너 차원에서 제공해주기도 한다.)

- **애플리케이션 컨텍스트는 빈을 검색하는 다양한 방법올 제공한다**

  (애플리케이션 컨텍스트의 getBean() 메소드는 빈의 이름을 이용해 빈을 찾아준다.  

  타입만으로 빈을 검색하거나 특별한 애노태이션 설정이 되어 있는 빈을 찾을 수도  있다. )

## 싱글톤 레지스트리와 오브젝트 스코프

> 우리가 만들었던 오브젝트 팩토리와 스프링의 애플리케이션 컨텍스트의 동작방식에는 무엇인가 차이점이 있다. 
>
> 스프링은 **여러 번에 걸쳐 빈을 요청하더라도 매번 동일한 오브젝트를 돌려준다는 것이다. **
>
> 단순하게 getBean() 을 실행할 때마다  userDao( ) 메소드를 호출하고 매번 new에 의해 새로운 UserDao가 만들어지지 않는다.
>
> **스프링은 기본적으로 별다른 설정을 하지 않으면 내부에서 생성하는 빈 오브젝트를  모두 싱글톤으로 만든다.**
>
> **> 스프링이 주로 적용되는 대상이 자바 엔터프라이즈 기술을 사용하는 서버환경이기 때문이다.**
>
> **> 매번 클라이언트 요청이 올때마다 각 로직을 담당하는 오브젝트를 새로 만들경우 한번에 많은 요청이 들어올경우 서버 과부하**

***

> **자바의 기본적인 싱글톤 패턴의 구현 방식은 여러 가지 단점이 있기 때문에，**
>
>  **스프링은 직접 싱글톤 형태의 오브젝트를 만들고 관리하는 기능을 제공한다. 그것이 바로 싱글톤 레지스트리다.**
>
> 기본적으로 private 생성자를 사용해야하는 싱글톤 패턴과 다르게 ,  평범한 자바 클래스를 싱글톤으로 활용하게 해준다 .

- 싱글톤 패턴과 달리 스프링이 지지하는 객체지향 적인 설계 방식과 원칙 , 디자인패턴(싱글톤 제외)등을 적용하는데 아무 제약이 없다.

> . **-> 스프링은 IoC 컨테이너일 뿐만 아니라 고전적인 싱글톤 패턴을 대신해서 싱글톤을 만들고  관리해주는 싱글톤 레지스트리다.**

***

> **기본적으로 싱글톤이 멀티스레드 환경에서 서비스 형태의 오브젝트로 시용되는 경우에는 **
>
> **상태정보를 내부에 갖고 있지 않은 무상태  stateless 방식으로 만들어져야 한다.**
>
> ( 저장할  공간이 하나뿐이니 서로 값을 덮어쓰고 자신이 저장하지 않은 값을 읽어올 수 있기 때문이다.)

> 예시 코드 

```java
public class UserDao {
		private ConnectionMaker connectionMaker; //읽기 전용은 상관 x 
		private Connection c; //매번 새로운 값으로바뀌는 정보를 담은 인스턴스 변수
		private User user; 
    
		public User get(String id) throws ClassNotFoundException, SQLException ( 
			this.c = connectionMaker.makeConnection(); 
			this.user = new User(); 
			this.user.setld(rs.getString(‘ id') ); 
			this.user.setName(rs.getString(‘ name')) ; 
			this.user.setPassword(rs.getString('password')); 
                                           
			return this.user;
	}
}
```

***

## 빈의 스코프

> 스프링이 관리하는 오브젝트 , 즉 빈이 생성되고 존재하고 , 적용 되는 범위를 스코프라고 한다.
>
> 기본스코프는 싱글톤 이다 .  강제로 제거하지 않는 한 스프링 컨테이너가 존재하는동안 계속 유지 된다 .

***

> 경우에 따라서 싱글톤 외의 스코프를 가질수 있다 .

- 프로토타입 -  싱글톤과 달리 컨테이너에 빈을 요청할 때마다  매번 새로운 오브젝트를 만들어준다 .
- 요청 스코프 -  웹을 통해 새로운 HTTP 요청이 생길  때마다 생성
- 세션 스코프 - 웹의 세션과 스코프가 유사

***

## 의존관계 주입

> 의존 관계란 ? 의존 대상이 기능이 추가 되거나 바뀌면 그영향이 의존하는 곳으로 전달된다는 것이다 .
>
> ( a가 b에 의존할경우 , b는 a가 바뀌어도 영향을 받지 않는다. )

> 프로그랩이 시작되고 UserDao 오브젝트가 만들어지고 나서 런타임 시에 의존관계  를 맺는 대상， 
>
> 즉 **실제 사용대상인 오브젝트를 의존 오브젝트**라고한다 .
>
> 의존관계 주입은 이렇게 구체적인 의존 오브젝트와 그것을 사용할 주체 , 
>
> 보통 클라이언트라고 부르는 오브젝트를 런타임시에 연결해주는 작업을 말한다 .
>
> **의존관계 주입의 핵심은 설계 시점에서는 알지 못했던 두 오브젝트의 관계를 맺도록 도와주는 제3의 존재가 있다는 것이다.**
>
> 제 3의 존재는 바로 관계설정 책임을 가진 코드를 분리했던 오브젝트 (ex - 앞서 만들었던 DaoPactory ) 라고 볼수 있다.
>
> DI컨테이너는 UserDao를 만드는 시점에서 생성자의 파라미터로 이미 만들어진 DConnectionMaker의 오브젝트를 전달한다.
>
> 정확히는 DConnectionMaker 오브젝트의 레퍼런스가 전달 되는 것이다 .
>
> **주입이라는건 외부에서 내부로 무엇인가를 넘겨줘야 하는 것인데 , ** 자바에서 오브젝트에 무언가를 넣어준다는 개념은 메소드를 실행하면서 파라미터로 오브젝트의 레퍼런스를 전달해주는 방법 뿐이다 . 가장 손쉽게 사용 할수 있는 파마미터 전달이가능한 메소드는 바로 **생성자**이다.
>
> DI컨테이너는 자신이 결정한 의존관계를 맺어줄 클래스의 오브젝트를 만들고 이생성자의 파라미터로 오브젝트의 레퍼런스로 전달해준다 . 이렇게 전달 받은 오브젝트는 인스턴스 변수에 저장해 둔다 .

```java
public class UserDao {
		private ConnectionMaker connectionMaker; 
		public UserDao(ConnectionMaker connectionMaker) { 
			this.connectionMaker = connectionMaker;
		}
		...//생략
}
```

> 이렇게 컨테이너에 의해 런타임시에 의존 오브젝트를 사용 할수 있도록 그 레퍼런스를 전달 받는 과정이 메소드(생성자)를 통해
>
> DI컨테이너가 UserDao에게 주입해주는 것과 같다고 해서 이를 의존 관계 주입이라고 부른다 .
>
> **DI는 자신이 사용할 오브젝트에 대한 선택과 생성 제어권을 외부로 넘기고 자신은 수동적으로 주입받은 오브젝트를 사용** 한다는점에서 IOC의 개념에 잘들어 맞는다 . 스프링 컨테이너의 IOC는 주로 의존관계 주입또는 DI라는데 초점이 맞춰져있다 .

***

## 의존관계 검색과 주입

> 스프링이 제공하는 IOC 방법에는 의존관계 주입만 있는것이 아니다. 코드에서는 구체적인 클래스에 의존 하지 않고 , 런타임 시에 의존 관계를 결정 한다는 점에서 의존관계주입이랑 비슷하지만 **의존관계를 맺는 방법이 외부로 부터의 주입이 아니라 스스로 검색을 이용하기 때문에 의존 관계 검색** 이라고한다 .
>
> 의존 관계 검색은 자신이 필요로하는 의존 오브젝트를 능동적으로 찾는다. 물론 자신이 어떤 클래스의 오브젝트를 이용 할지는 결정하지는 않는다 .그러면 IOC라고 할수 는 없을 것이다 . 
>
> **의존관계 검색은 런타임시 의존관계를 맺을 오브젝트를 결정하는 것과 오브젝트의 생성 작업은 외부 컨테이너에게 IOC로 맡기지만** , 이를 가져올때는 **메소드나 생성사를 통한 주입대신 스스로 컨테이너 에게 요청**하는 방법을 사용한다 .

```java
public UserDao(){
	DaoFactory daoFactory = new DaoFactory();
	this.connectionMaker = daoFatory.connectionMaker();
    //스스로 ioc 컨테이너인 DaoFatort에 요청 
}
```

> DaoDactory의 경우라면 미리 준비된 메소드를 호출 하면 되니까 단순히 요청으로 보이겠지만,
>
> 이런작업을 일반화한 스프링의 애플리케이션 컨텍스트라면 미리 정해놓은 이름을 전달해서 그 이름에 해당하는 오브젝트를 찾게된다 . 따라서 이를 일종의 검색이라고 볼수 있다 . 
>
> 또한 그 대상이 런타임 의존관계를 가질 오브젝트 이므로 의존 관계 검색이라고 부르는것이다 .

***

> 스프링 ioc 컨테이너인 애플리케이션 컨텍스트는 **getBean()이라는 메소드**를 제공한다 . 
>
> **바로 이 메소드가 의존관계 검색에 사용되는것이다 .**
>
> UserDao는 아래와 같은 방식으로 connectionMaker오브젝트를 가져오게 만들 수도 있다.

```java
public UserDao(){
	AnnotationConfigApplicationContext context = 
		new AnnotationConfigApplicationContext(DaoFactory.class);
	this.connectionMaker = context.getBean("connectionMaker",ConnectionMaker.class);
}
```

> 의존 관계 검색은 기존 의존관계 주입의 거의 모든 장점을 갖고 있다 . ioc원칙에도 잘들어 맞는다 . 단 방법만 조금 다를뿐이다.
>
> 관계 검색과 주입중에 어떤 것이 더 나을까 ? -> 의존관계 주입쪽이 훨씬 단순하고 깔끔하다 .
>
> 하지만 의존관계 검색을 사용 해야 할때가 있다 . 
>
> 애플리 케이션의 기동 시점에서 적어도 한번은 의존관계 검색방식을 사용해 오브젝트를 가져와야한다 .
>
>  ( main()에서는 DI 를 이용해 오브젝트를 주입받을 방법X )
>
> 서버도 마찬가지로 기동메서드( main() )은 없지만 사용자의 요청을 받을 때마다 main()과 비슷할 역할을 하는 서블릿에서
>
> 스프링 컨테이너에 담긴 오브젝트를 사용 하려면 한번은 의존관계 검색을 사용해 오브젝트를 가져와야한다.
>
> 다행히 이런 **서블릿은 스프링이 미리 만들어서 제공하기 때문에 ** 직접 구현할 필요는 없다 .

***

> **의존 관계 검색과 주입의 차이점**
>
> 검색 -> 검색하는 오브젝트는 자신이 스프링의 빈일 필요가 없다 .
>
> 주입 -> 검색하는 오브젝트도 빈이여야한다 . ( 자기 자신이 먼저 컨테이너 관리하는 빈이 되어야함. )

***

> **의존관계 주입의 장점**
>
> 코드에는 런타임 클래스에 대한 의존관계가 나타나지 않음 .
>
> 인터페이스를 통해 결합도가 낮은코드를 만들므로 , 다른 책임을 가진 사용관계에 있는 대상이 바뀌거나 변경 되더라도 
>
> 자신은 영향을 받지 않으며 **변경을 통한 확장 방법에 자유롭다. **

> 예시코드1 ( DB를 로컬에서 서버 배포용 연결로 변경 하고싶을 경우 )

```java
@Bean
public ConnectionMaker connectonMaker(){
	return new LocalDBConnectionMaker();
}	
```

```java
@Bean
public ConnectionMaker connectonMaker(){
	return new ProductionDBConnectionMaker(); // 한줄만 수정하면됨
}	
```

>예시코드2 
>
>DAO가 DB를 얼마나 많이 연결해서 사용하는 파악하고 싶을경우 무식한 방법으로 모든 DAO의 makeConnection() 메소드를 호출 하는 부분에 새로 추가한 카운터를 증가시키는 코드를 넣어야할까 ? 
>
>-> DAO와 DB 커넥션을 만드는 오브젝트 사이에 연결 횟수를 카운팅하는 오브젝트를 하나더 추가하는것이다.
>
>( 기존 코드를 수정하지 않고 컨테이너가 사용 하는 설정 정보만 수정해서 런타임 의존관계만 새롭게 지정하면 된다 . )

```java
public class CountingConnectionMaker implements ConnectionMaker ( 
    
		int counter = Ð; 
		private ConnectionMaker realConnectionMaker; 
    
		public CountingConnectionMaker(ConnectionMaker realConnectionMaker) { 
			this.realConnectionMaker = realConnectionMaker; 
        }
    
		public Connection makeConnection() throws ClassNotFoundException, SQLException { 
			this.counter++;
            return realConnectionlaker.makeConnection(); 
        }
            
		public int getCounter() { 
			return this.counter;
		}
    }
```

> ConnectionMaker인터페이스를 구현했지만 내부에서 DB커넥션을 만들지 않는다 .
>
> DAO가 DB커넥션을 가져올때마다 호출하는 makeConnection()에서 DB연결 카운터를 증가시킨다 .
>
> 카운팅 작업을 마치면 실제 DB커넥션을 만들어주는 realConnectionlaker에 저장된 ConnectionMaker 타입 오브젝트의 
>
> makeConnection() 를 호출해서 그결과를 DAO에게 돌려준다 .

```java
@Configuration 
public class CountingDaoFactory { 
		@Bean
		public UserDao userDao() { 
			return new UserDao(connectionMaker()); 
		}
		@Bean
		public ConnectionMaker connectionMaker() {
			return new CountingConnectionMaker(realConnectionMaker()); 
 		}	
 		@Bean
		public ConnectionMaker realConnectionMaker() ( 
			return new DConnectionMaker()
		}
}
```

> 사용코드

```java
public class UserDaoConnectionCountingTest {
		public static void main(String[) args) throws ClassNotFoundException, SQLException {
            
		AnnotationConfigApplicationContext context = 
				new AnnotationConfigApplicationContext(CountingDaoFactory.class); 
		UserDao dao = context.getBean("userDao" , UserDao .class); 

        //Dao사용 코드 
		CountingConnectionMaker ccm = context.getBean("connectionMaker" ’ CountingConnectionMaker.class); 
        //의존관계 검색으로 어떤 빈이든 가져올수 있다 .
		System.out.println("Connection counter "+ ccm.getCounterO);
		}
}
```

***

## 메소드를 이용한 의존관계 주입

> 여태까지 생성자로 의존 관계를 주입했는데 , 일반 메소드로 주입하는 방법도 있다 .

- 수정자 (setter) 를 이용한 주입 
- 일반메소드를 이용한 주입 ( 한번에 여러개의 피라미터를 받을수 있다. 하지만 피라미터의 갯수가 많아지고 비슷한 타입이 여러개라면 실수하기 쉽다  . 적절한 개수의 피라미터를 가진 여러개의 초기화 메소드를 만들수 있기때문에 한번에 모든 피라미터를 받아야하는 생성자보다 낫다 )

> 스프링은 전통적으로 수정자 메소드를 가장 많이 사용해 왔다 . 뒤에서 보겠지만 DaoFactory같은 자바코드보다  xml 을 사용하는경우 에는 자바빈 규약을 따르는 수정자 메소드가 가장 사용하기 편리하다.

```java
public class UserDao {
    	private ConnectionMaker connectionMaker; 
		//수정자메소드를 이용한 예시 
		public void setConnectionMaker(ConnectionMaker connectionMaker) { 
			this.connectionMaker = connectionMaker;
		}
}
```

```java
@ßean 
public UserDao userDao() { 
		UserDao userDao = new UserDao(); 
		userDao.setConnectionMaker(connectionMaker()); 
		return userDao;
}
```

