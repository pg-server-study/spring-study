# 트랜잭션 

트랜잭션이라고 모두 같은 방식으로 동작하는 것은 아니다 물론 트랜잭션의 기본 개념인 더이상 쪼갤수 없는 최소단위의 작업이라는 개념은 항상 유효하다.

따라서 트랜잭션 경계 안에서 진행된 작업은 commit () 을 통해 모두 성공하던지  아니면 rollback() 을 통해 모두 취소되어야한다. 

그런데 이밖에도 트랜잭션의 동작방식을 제어 할 수 있는 몇가지 조건이있다.

## 트랜잭션 전파

트랜잭션의 경계에서 이미 진행중인 트랜잭션이 있을 떄 또는 없을때 어떻게 동작 할것인가를 결정하는 방식을 말한다.

트랜잭션 전파와 같이 각각 독립적인 트랜잭션 경계를 가진 두개의 코드가 있다고 하자 . 그런데 a의 트랜잭션이 시작되고 아직 끝나지 않은 시점에서 b를 호출 했다면 b의 코드는 어떤 트랜 잭션 안에서 동작해야 할까?

이렇게 독자적인 트랜잭션 경계를 가진 코드에 대해 이미 진행 중인 트랜잭션이 어떻게 영향을 미칠 수 있는가를 정의 하는것이 트랜잭션  전파 속성이다.

대표적으로 다음과 같은 트랜잭션 전파 속성을 줄 수있다.

- PROPAGATION_REQULRED

  가장 많이 사용되는 트랜잭션 전파 속성,  진행중인 트랜잭션이 없으면 새로 시작하고 , 이미 시작된 트랜잭션이 있으면 이에 참여한다.

- PROPAGATION_REQUIRES_NEW

  항상 새로운 트랜잭션을 시작한다. 즉 앞에서 시작된 트랜잭션이 있든 없든 상관없이 새로운 트랜잭션을 만들어서 독자적으로 동작하게 한다.

- PROPAGATION_NOT_SUPPPORTED

  이 속성을 사용하면 트랜잭션 없이 동작 하도록 만들 수도 있다.

  그냥 트랜잭션 경계설정을 하지 않으면 되는게 아닐까 할수 있지만 , 트랜잭션 경계 설정은 보통 AOP를 이용해 한번에 많은 메소드에 동시에 적용하는 방법을 사용한다. 그런데 구중에서 특별한 메소드만 트랜잭션 적용에서 제외하려면 어떻게 해야할까? 모든 메소드에 트랜잭션 AOP가 적용 되게하고, 특정 메소드의 트랜잭션 전파 속성만 설정해서 트랜잭션 없이 동작하게 만드는 편이 낫다.

### 격리수준

모든 트랜잭션은 격리수준을 갖고 있어야한다. 서버환경에서 여러개의 트랜잭션이 동시에 진행될 수 있다 . 가능하다면 모든 트랜잭션이 순차적으로 진행돼서 다른 트랜잭션의 작업에 독립적인 것이 좋겠지만, 그러자면 성능이 크게 떨어질 수 밖에없다. 따라서 적절하게 격리 수준을 조정해서 가능한 한 많은 트랜잭션을 동시에 진행 시키면서도 문제가 발생하지 않게 제어가 필요하다. 

격리 수준은기본적으로 DB에 설정되어 있지만 JDBC 드라이버나 DataSource 등에서 재설정 할 수 있고, 필요하다면 트랜잭션 단위로 격리 수준을 조정 할 수 있다.

기본적으로는 DB나 DataSource에 설정된 디폴트 격리 수준을 따르는것이 좋지만 특별한 작업을 수행하는 메소드의 경우는 독자적인 격리 수준을 지정할 필요가 있다.

### 제한시간

트랜잭션을 수행하는 제한시간을 설정 할수 있다 . DefaultTransactionDefinition의 기본설정은 제한시간이 없는것이다.

### 읽기 전용

읽기 전용으로 설정해두면 트랜잭션 내에서 데이터를 조작하는 시도를 막아줄수 있다 . 또한 데이터 액세스 기술에 따라서 성능이 향상 될 수도있따.

트랜잭션의 정의를 수정하려면 어떻게 해야 할까? 외부에서 정의된 TransactionDefinition 오브젝트를 DI받아서 사용하도록 만들면된다.

## 트랜잭션 인터셉터와 트랜잭션 속성

메소드별로 다른 트랜잭션 정의를 적용하려면 어드바이스의 기능을 확장해야한다 . 마치 초기에 TransactionHandler 에서 메소드 이름을 이용해 트랜잭션 적용 여부를 판단 했던 것과 비슷한 방식을 사용하면 된다. 메소드 이름 패턴에 따라 다른 트랜잭션 정의가 적용되도록 만드는것이다.

### Transactioninterceptor

이를위해 기존에 만들었던 TransactionAdvice를 다시 설계할 필요는 없다 . 이미 스프링에는 편리하게 트랜잭션 경계설정 어드바이스로 사용 할 수 있도록 만들어진 TrancactionInterceptor가 존재하기 떄문이다. 

```java
public Object invoke(MethodInvocation invocation)throws Throwable{
	TransactionStatus status =
		this.transactionManager.getTransaction(new DefaultTransactionDefinition());
		try{
			Object ret = invocation.proceed();
			this.transactionManager.commit(status);
			return ret;
		}catch(RuntimeException e){
			this.transactionManager.rollback(status);
			throw e;
		}
}
```

TransactionAdvice는 RuntimeException 이 발생하는 경우에만  트랜잭션을 롤백 시킨다 . 하지만 런타임 예외가 아닌 경우에는 트랜잭션이 제대로 처리되지 않고 메소드를 빠져나가게 되어 있다 .

스프링이 제공하는 Transactionlnterceptor에는 기본적으로 두 가지 종류의 예외 처리 방식이 있다. 런타임 예외가 발생하면 트랜잭션은 롤백된다, 체크예외를 던지는 경우에는 이것을 예외 상황으로 해석하지 않고 일종의 비즈니스 로직에 따른 , 의미가 있는 리턴 방식의 한 가지로 인식해서 트랜잭션을 커밋해버린다.

그런데 ransactionlnterceptor의 이러한 예외처리 기본 원칙을 따르지 않는 경우가 있을 수도 있다 .

**그래서 TransactionAttribute는 rollbackOn()이라는 속성을 둬서 기본 원칙과 다른 예외 처리가 가능하게 해준다. 이를 활용하면 특정 체크 예외의 경우는 트랜잭션을 롤백시키고 , 특정 런타임 예외에 대해서는 트랜잭션을 커밋시킬수도 있다.**

### 메소드 이름 패턴을 이용한 트랜잭션 속성 지정

Properties타입의 transactionAttributes 프로퍼티는 메소드 패턴과 트랜잭션 속성을 키와 값으로 갖는 컬렉션이다. 이중에서 트랜잭션 전파 항목만 필수이고 나머지는 다 생략 가능하다 . 생략하면 모두 DefaultTransactionDefinition에 설정된 디폴트 속성이 부여된다.

> 메소드 이름 패턴과 문자열로 된 트랜잭션 속성을 이용해서 정의한 TransactionInterceptor 타입 빈의 예다.

```xml
<bean id="transactionAdvice" 
class="org.springframework.transaction.interceptor.Transactionlnterceptor"> 
	<property name="transactionManager" ref="transactionManager" />
	<property name="transactionAttributes">
		<props>
			<prop key="get*">PROPAGATION_REQUIRED,readOnly , timeout_30</prop> 
			<prop key=“upgrade*">PROPAGATION_REQUIRES_NEW, ISOLATION_SERIALIZABLE 
			</prop>
			<prop key="*">PROPAGATION_REQUIRED</prop>
		</props>
	</property>
</bean>
```

> spring boot configuration  설정 방법
>
> [참고 블로그 ](https://linked2ev.github.io/gitlog/2019/10/02/springboot-mvc-15-%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-MVC-Transaction-%EC%84%A4%EC%A0%95/)

```java
@Contiguration
public class TransactionAspect{
	@Autowired
	platformTransactionManager transactionManager;
	
	@Bean
	public TransactionInterceptor transactionAdvice() {
		NameMatchTransactionAttributeSource txAttributeSource = new NameMatchTransactionAttributeSource();
		RuleBasedTransactionAttribute txAttribute = new RuleBasedTransactionAttribute();
        //지정된 예외가 양수 및 음수 모두에 대해 여러 롤백 규칙을 적용하여 트랜잭션 롤백을 유발해야 하는지 여부를 확인하는 TransactionAttribute 구현입니다. 사용자 지정 롤백 규칙이 적용되지 않는 경우 이 특성은 DefaultTransactionAttribute(런타임 예외 시 롤백)처럼 작동
		
		txAttribute.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Exception.class)));
		txAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		
		HashMap<String, TransactionAttribute> txMethods = new HashMap<String, TransactionAttribute>();
		txMethods.put("*", txAttribute);
		txAttributeSource.setNameMap(txMethods);

		return new TransactionInterceptor(transactionManager, txAttributeSource);
	}
```

