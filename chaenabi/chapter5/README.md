노션 링크: https://reminiscent-headlight-ee3.notion.site/5-b2cd9c8c907d4cbda0455242f01bd26a

# 5장

Created: October 31, 2021 9:26 AM
Tags: 백엔드 스터디, 서비스 추상화

## String 대신 enum 사용하기 (User::level)



```java
@Getter
@RequiredArgsConstructor 
public class User {
   private final String id;
   private final String name;
   private final Level level; // User 클래스에 Level 데이터 추가.
              // level을 String으로 관리해도 되지만 Enumerate 타입으로 관리하면 장점이 많다.
}

public enum Level {
   BASIC(SILVER), SILVER(GOLD), GOLD(null);
   private Level higherLevel;
   
   public Level(Level higherLevel) {
      this.higherLevel = higherLevel;
   }
   
   // 레벨 수정 기능
   public Level upgradeLevel(Level level) {
       return level.higherLevel;
   }
   
   public static Level valueOf(String value) {
      switch (value) {
         case "BASIC": return BASIC;
         case "SILVER": return SILVER;
         case "GOLD": return GOLD;
         default: throw new IllegalArgumentException("this "+value+" does not support."); 
      }
   }
}
```

위의 User와 Level을 사용하는 코드

```java

public void insertUser() {
   UserDao dao = new UserDao();
   dao.add(new User("gyumee", "박성철", Level.BASIC));
   dao.add(new User("leegw700", "이길원", Level.SILVER));
   dao.add(new User("bumjin", "박범진", Level.GOLD));
   
   // enum을 사용하면 level 컬럼에 제한된 값만 넣을 수 있으며,
   // 오타가 발생할 수 있는 문자열에 비해 enum 타입은
   // 명확한 값으로 전달할 수 있기 떄문에 편리하다.
}

public class UserDao {
   @Autowired
   private final JdbcTemplate jdbcTemplate;

   public void add(User user) {
      this.jdbcTemplate.update("insert into user values(?, ?, ?)",
                        user.getId(), user.getName(), String.valueOf(user.getLevel()));
      // enum 타입은 object이므로 문자열로 변환하여 database에 저장한다.
      // 대부분의 서드파티 database api들은 enum 타입의 값을 자동으로 정수 타입 또는
      // 문자열로 변환하는 기능을 지원하기 떄문에 valueOf() 메서드처리가 필수는 아니다.
   }

   // 레벨 관리 비즈니스 로직
   public void upgradeLevel(User user) {
      this.jdbcTemplate.update("update user set level = ? where id = ?",
                           Level.upgradeLevel(user.getLevel()), user.getId());
   }
}

```



## 트랜잭션 서비스 추상화

---

모든 유저에 대한 level을 상승시키는 작업 도중 네트워크가 끊기면 어떻게 해야할까?

이미 처리된 유저와 처리되지 못한 유저를 구분하기 어렵기 때문에

해당 작업을 처음으로 완전히 되돌려 취소시키는 방법을 선택했다고 가정해보자.

`모든 데이터를 작업 전의 값으로 되돌리려면` 해당 `작업이 한 트랜잭션으로 묶여있어야 한다.`

그러기 위해서는 일련의 작업들(유저 한명한명에 대한 level 상승 메서드 실행)이

하나의 DB 커넥션 안에서 수행되어야 한다. 

그런데 JdbcTemplate의 메서드들 (query(), update() 등)은 호출 시

자체적으로 데이터베이스 연결과 로직 수행, 종료까지 모두 담당하기 때문에

외부에서 DB 커넥션을 제어할 수 있는 방법이 없어보인다.

이러한 문제를 해결하기 위해 JdbcTemplate은 트랜잭션 동기화 방식을 지원하고 있다.

트랜잭션 동기화 기능을 아래와 같이 사용할 수 있다.

```java
public class UserDao {
   @Autowired
   private final JdbcTemplate jdbcTemplate;

   // Connection을 생성할 때 사용할 Database source를 DI받도록 한다.
   @Autowired
   private final DataSource datasource; 

   public void upgradeLevel(User user) {
      // 트랜잭션 동기화 관리자를 이용해 동기화 작업을 초기화한다.
      TransactionSynchronizationManager.initSynchronization();
      // DB 커넥션을 생성하고 트랜잭션을 시작시킨다. 
      Connection c = DataSourceUtils.getConnection(this.dataSource);
      c.setAutoCommit(false); // update sql이 실행될때마다 자동 commit 되는 것을 비활성화.
      try {
        List<User> users = getAll();
        for (User user : users) {
            this.jdbcTemplate.update("update user set level = ? where id = ?",
                           Level.upgradeLevel(user.getLevel()), user.getId());
        }
      } catch (RuntimeException e) {
         c.rollback(); // 예외가 발생하면 해당 트랜잭션을 모두 롤백시켜 모든 작업을 
                       // 일괄 취소시켜, 모든 데이터를 작업 전으로 되돌린다.
         throw e;
      }
      // 스프링 유틸리티 메소드를 이용해 DB 커넥션을 안전하게 닫는다.
      DataSourceUtils.releaseConnection(c, dataSource); 
      // 동기화 작업 종료 및 정리
      TransactionSynchronizationManager.unbindResource(this.dataSource);
      TransactionSynchronizationManager.clearSynchronization();
   }

   public List<User> getAll() {
       return this.jdbcTemplate.query("select * from users order by id",
           new RowMapper<User>() {
	      public User mapRow(ResultSet rs, int rowNum) throws SQLException { 
	         User user = new User(); 
                 user.setId(rs.getString("id")); 
                 user.setName(rs.getString("name")); 
                 user.setLevel(Level.valueOf(rs.getString("level")));
                 return user;
             }
	  });
    }
}
```
