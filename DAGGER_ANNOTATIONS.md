# Dagger2 注解手册（附可运行测试）

> 本文档配套一个**能点着跑的测试 App**：`testdemo` → 首屏「6. Dagger2 注解测试（30 项）」。
> 30 个测试项对应 34 个注解（部分注解成对出现），共 **66 条断言，在真机上实测全部通过**。
>
> 代码位置：`app/src/main/java/com/example/testdemo/dagger/`
> 说明：这里的代码**不是示例片段**，就是 App 里在跑的那份；每个 `Item*.java` 里的 `static List<T.Row> xxx()` 就是一个测试项。

---

## 0. 怎么跑

1. 打开 App → 测试功能列表 → **6. Dagger2 注解测试（30 项）**
2. 顶部「全部运行」，30 项一次跑完，顶部显示 `已跑 30/30 项 · 全部通过（66 条断言）`
3. 也可以单项「运行」，展开的 `✓ / ✗` 是每一条断言

不想开界面，也可以在电脑上跑（纯 JVM，不需要模拟器）：把 `dagger/` 下 8 个文件 + 下面这段 Runner 一起用 `javac` 编，用 `java` 跑。

```java
// 与 App 里完全相同的入口
for (Registry.Item item : Registry.all()) {
    List<T.Row> rows = item.run();
    System.out.println(item.ann + " / " + item.title + " -> " + rows.size() + " checks");
}
```

### 依赖配置

```toml
# gradle/libs.versions.toml
[versions]
dagger = "2.51"

[libraries]
dagger          = { group = "com.google.dagger", name = "dagger",          version.ref = "dagger" }
dagger-compiler = { group = "com.google.dagger", name = "dagger-compiler", version.ref = "dagger" }
javax-inject    = { group = "javax.inject",     name = "javax.inject",     version = "1" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.dagger)
    implementation(libs.javax.inject)
    annotationProcessor(libs.dagger.compiler)   // 纯 Java 源文件，不需要 kapt
}
```

> 本项目测试代码是 **Java**，所以用 `annotationProcessor` 就够了。如果 Dagger 代码写在 Kotlin 里，要换成 `kapt`（或 `ksp`）。

---

## 1. 注解总表

| # | 注解 | 一句话 | 测试项 |
|---|---|---|---|
| 1 | `@Inject` | 标在构造/字段/方法上，告诉 Dagger「怎么建、往哪灌」 | 构造注入 / 成员注入 |
| 2 | `@Component` | 一张依赖图的入口，接口方法就是图的出口 | 整张图的入口 |
| 3 | `@Singleton` | 组件内唯一的缓存 | 组件内唯一 |
| 4 | `@Scope` | 元注解，用来定义自己的作用域 | 自定义作用域 |
| 5 | `@Reusable` | 「可复用」但**不保证**唯一 | 可复用 |
| 6 | `dagger.Lazy` | 延迟创建 + 之后复用 | 延迟 + 缓存 |
| 7 | `Provider<T>` | 每次 `get()` 都新建 | 每次新建 |
| 8 | `@Module` | 绑定容器，`includes` 可合并 | 绑定容器 |
| 9 | `@Provides` | 手写工厂方法 | 手写工厂 |
| 10 | `@Binds` | 接口 → 实现的零成本别名 | 接口别名 |
| 11 | `@BindsOptionalOf` | 声明「可能没有」 | 可选能力 |
| 12 | `@BindsInstance` | 把运行期的值塞进图 | 运行期入参 |
| 13 | `@Component.Factory` | 推荐的无参组件构建方式 | 组件工厂 |
| 14 | `@Component.Builder` | 旧写法，与 Factory 等价 | 组件 Builder |
| 15 | `@Component(dependencies)` | 组件依赖（只用对方暴露的类型） | 组件依赖 |
| 16 | `@Qualifier` | 自定义限定符 | 自定义限定符 |
| 17 | `@Named` | 字符串限定符 | 字符串限定符 |
| 18 | `@Subcomponent` | 子组件，单向继承父图 | 子组件 |
| 19 | `@Subcomponent.Factory` | 子组件工厂 | 子组件工厂 |
| 20 | `@Subcomponent.Builder` | 子组件 Builder（旧写法） | 子组件 Builder |
| 21 | `@Module(subcomponents)` | 把子组件登记进父图 | 登记子组件 |
| 22 | `@IntoSet` | 往 Set 里加一个 | 集合多绑定 |
| 23 | `@ElementsIntoSet` | 往 Set 里加一批 | 批量入集合 |
| 24 | `@IntoMap` | 往 Map 里加一键值对 | 表多绑定 |
| 25 | `@StringKey` `@IntKey` `@LongKey` `@ClassKey` | 内置 MapKey | 内置 MapKey |
| 26 | `@MapKey` | 自定义 MapKey | 自定义 MapKey |
| 27 | `@Multibinds` | 允许空 Set / Map | 空集合声明 |
| 28 | `@AssistedInject` | 构造里区分「图给的」和「现场给的」 | 现场参数 |
| 29 | `@Assisted` | 标记「现场给的」参数 | 现场参数 |
| 30 | `@AssistedFactory` | 只声明 `@Assisted` 参数的工厂 | 辅助工厂 |

---

## 2. 注入与作用域

### 2.1 `@Inject` —— 构造注入

**作用**：标在构造函数上，Dagger 需要这个类型时自己 `new` 并把参数注入进去。

```java
static class Engine {
    static int created = 0;
    @Inject Engine() { created++; }
}

static class Car {
    final Engine engine;
    @Inject Car(Engine engine) { this.engine = engine; }   // 参数就是依赖
}

@Singleton @Component
interface Cmp { Car car(); }
```

```java
Car car = DaggerItemInject_Cmp.create().car();
```

**断言**：`car != null`、`构造参数 engine 被自动注入`。

**坑**：
- 一个类只能有**一个** `@Inject` 构造。
- `@Inject` 构造不能是 `private`（生成代码在另一个类里调）。

### 2.2 `@Inject` —— 字段 / 方法注入（成员注入）

**作用**：给**已经存在的对象**灌依赖。Activity、View、Fragment 这类「系统 new 的」只能这么办。

```java
static class Garage {
    @Inject Engine fieldEngine;      // 字段注入：不能 private / final
    Engine methodEngine;

    @Inject void setMethodEngine(Engine e) { this.methodEngine = e; }
}

@Singleton @Component
interface Cmp {
    void inject(Garage garage);      // 成员注入入口：参数是已存在的对象
}
```

```java
Garage g = new Garage();
t.ok("注入前字段为 null", g.fieldEngine == null);
cmp.inject(g);                        // 灌进去
```

**断言**：注入前为 `null` → 注入后非 `null`（字段、方法各一条）。

**坑**：如果只写 `Garage garage();` 而不给 `@Inject` 构造，编译期会报
`[Dagger/MissingBinding] Garage cannot be provided without an @Inject constructor ... This type supports members injection but cannot be implicitly provided.`
—— **成员注入 ≠ 可被提供**，入口必须是 `void inject(T)`。

### 2.3 `@Component` —— 图的入口

**作用**：声明一张依赖图。抽象方法（getter）是图的出口；`void inject(T)` 是成员注入入口。

```java
@Singleton
@Component
interface Cmp {
    Car car();
    void inject(Garage garage);
    Counter counter();
}
```

```java
Cmp cmp = DaggerItemInject_Cmp.create();   // 无参组件用 create()
```

**断言**：`create()` 能拿到组件；组件方法能取到对象；同一组件反复取不会重建组件。

**坑**：组件接口名 `Cmp` + 外层类 `ItemInject` → 生成类叫 `DaggerItemInject_Cmp`。**嵌套多少层，下划线就多长**，改名就等于改生成类名。

### 2.4 `@Singleton` —— 组件内唯一

**作用**：把绑定缓存到组件生命周期内。它是**作用域**（`@Scope` 的特例），不是「进程单例」。

```java
@Singleton static class Counter { @Inject Counter() {} }
```

**断言**：两次取到同一实例；构造只发生一次。

**坑**：`@Singleton` 的组件不能引用别的作用域的绑定 →
`[Dagger/IncompatiblyScopedBindings]`。这就是本项目里 `Session` 必须单独开 `SessionCmp` 的原因。

### 2.5 `@Scope` —— 自定义作用域

**作用**：定义自己的作用域名，语义与 `@Singleton` 一样，只是「缓存范围」绑定到用它的组件。

```java
@Scope
@Retention(RetentionPolicy.RUNTIME)   // 想用反射检查就必须 RUNTIME
@interface LocalScope {}

@LocalScope static class Session { @Inject Session() {} }

@LocalScope @Component
interface SessionCmp { Session session(); Car car(); }
```

**断言**：`@Scope` 是元注解（`LocalScope` 上标着 `@Scope`）；`@Singleton` 也是 `@Scope`；`@LocalScope` 两次同一实例；无作用域的 `Car` 两次不同。

**坑**：作用域的唯一意义是「**同一个组件内**缓存」。跨组件不共享——想要跨组件共享，就把绑定往上提到父组件。

### 2.6 `@Reusable` —— 可复用

**作用**：比 `@Singleton` 弱：**可能**缓存，也可能不缓存，由 Dagger 决定（比如它认为这个绑定不会被复用，就直接每次新建）。

```java
@Reusable static class ReusableThing { @Inject ReusableThing() {} }
```

**断言**：能注入；并且**不要对实例身份做任何假设**（这条正是测试里明确写出来的）。

**什么时候用**：建起来贵、但语义上不要求唯一（如某些 `Typeface`、解析器）。作用域语义要求严格时别用它。

### 2.7 `dagger.Lazy<T>` —— 延迟 + 缓存

**作用**：注入时不创建，第一次 `get()` 才创建，之后复用同一个。

```java
static class Holder {
    final Lazy<Engine> lazy;
    final Provider<Engine> provider;
    @Inject Holder(Lazy<Engine> lazy, Provider<Engine> provider) { ... }
}
```

```java
Holder h = cmp.holder();
t.eq("拿到 Holder 时还没创建 Engine", 0, Engine.created);   // 延迟
Engine e1 = h.lazy.get();
Engine e2 = h.lazy.get();
t.ok("Lazy.get() 两次同一实例", e1 == e2);                  // 缓存
```

**典型用途**：破循环依赖（A 依赖 B、B 依赖 A 时，把其中一边改成 `Lazy`）。

### 2.8 `Provider<T>` —— 每次新建

**作用**：`get()` 每调一次就新建一个。

**断言**：`Provider.get()` 两次不同实例；创建计数 +2。

**和 `Lazy` 的区别**：`Lazy` = 「延迟 + 只建一次」；`Provider` = 「延迟 + 每次新建」。想要「延迟 + 只建一次 + 可重置」用 `Provider<Lazy<T>>`。

---

## 3. 绑定方式

### 3.1 `@Module` + `@Provides`

**作用**：`@Module` 是绑定容器；`@Provides` 是手写工厂方法，方法参数会自动被注入。

```java
@Module
static class ConfigMod {
    @Provides static String prefix() { return "v"; }

    @Provides @Named("version")
    static String version(String prefix) { return prefix + "1.0.0"; }   // 可以依赖别的绑定
}

@Module(includes = ConfigMod.class)      // 合并另一个 Module 的绑定
static class AppMod {
    @Provides static Integer retry() { return 3; }
}
```

**断言**：`@Provides` 里的逻辑被执行（`v1.0.0`）；`includes` 把 `ConfigMod` 的绑定合并进来了（`retry = 3`）。

**什么时候用 `@Provides`**：类改不了（三方库/框架类）、建对象时要写装配逻辑、返回值需要计算。

### 3.2 `@Binds` —— 接口别名

**作用**：只做「接口 → 实现」的类型映射，不 `new`、不写方法体。

```java
interface Svc { String name(); }
static class SvcImpl implements Svc {
    @Inject SvcImpl() {}
    public String name() { return "impl"; }
}

@Module
abstract static class BindsMod {
    @Binds abstract Svc bindSvc(SvcImpl impl);   // 方法必须 abstract
}
```

**断言**：注入方拿到的是实现类的行为（`impl`），且 `instanceof SvcImpl`。

**坑**：`@Binds` 方法必须 `abstract`，且所在 Module 必须是 `abstract class` 或 `interface`；参数类型必须自己能被注入（有 `@Inject` 构造或已被 `@Provides`）。

### 3.3 `@BindsOptionalOf` —— 可选能力

**作用**：声明「这个类型可能有、也可能没有」，注入方拿到 `Optional<T>`，缺实现时编译不失败。

```java
@Module
abstract static class OptMod {
    @BindsOptionalOf abstract Svc optionalSvc();     // 只声明，不提供实现
}

@Component(modules = OptMod.class)
interface OptAbsentCmp { Optional<Svc> svc(); }      // 编译通过，运行时 empty

@Component(modules = {OptMod.class, BindsMod.class})
interface OptPresentCmp { Optional<Svc> svc(); }     // 有实现时才 of(...)
```

**断言**：无实现 → `Optional.empty()`；有实现 → `Optional.of(impl)`。

**典型场景**：多形态设备（有/无 PIP、有/无折叠屏）、可插拔模块。SystemUI 里用了 21 处。

### 3.4 `@BindsInstance` + `@Component.Factory`

**作用**：把**运行期才知道**的值（用户名、Intent 参数、Activity 实例）塞进图，而且不用写 Module。

```java
@Component
interface FactoryCmp {
    Welcome welcome();

    @Component.Factory
    interface Factory {
        FactoryCmp create(@BindsInstance String user);   // 每个参数都要 @BindsInstance
    }
}
```

```java
Welcome w = DaggerItemModule_FactoryCmp.factory().create("alice").welcome();
t.eq("运行期传参进入依赖图", "alice", w.user);
```

**坑**：
- `@Component.Factory` 里**每个参数都必须**标 `@BindsInstance`，否则报错。
- 一个组件只能有一个 `Factory` 或 `Builder`，不能两个都有。
- `@BindsInstance` 的值在**这张图的生命周期内不可变**。需要变，就重新 create 一个组件。

### 3.5 `@Component.Builder` —— 旧写法

```java
@Component
interface BuilderCmp {
    Welcome welcome();

    @Component.Builder
    interface Builder {
        @BindsInstance Builder user(String user);
        BuilderCmp build();        // 必须有且只有一个返回组件的方法
    }
}
```

**断言**：与 Factory 等价（`carol`）。

**取舍**：**新代码用 `Factory`**，参数在 `create()` 里一次给全，不容易漏。

### 3.6 `@Component(dependencies = ...)` —— 组件依赖

**作用**：让 A 组件用 B 组件提供的绑定。跟 `@Subcomponent` 的区别是「**平级**、不是父子」，且只传递 B **显式暴露**的类型。

```java
@Component(modules = ClockMod.class)
interface DepCmp { Clock clock(); }              // 必须暴露，否则外面拿不到

@Component(dependencies = DepCmp.class)
interface DependentCmp { Timer timer(); }
```

```java
DepCmp dep = DaggerItemModule_DepCmp.create();
DependentCmp child = DaggerItemModule_DependentCmp.builder().depCmp(dep).build();
```

**断言**：依赖组件暴露的 `Clock` 被复用（`now() == 42`）。

**坑**：依赖组件的**作用域包含关系**必须成立（`@Singleton` 的组件不能依赖一个更短命的组件）。

---

## 4. 限定符

### 4.1 `@Qualifier`

**作用**：同一个类型有多份绑定时，用注解把它们区分开。Dagger 的 Key = `类型 + 限定符`。

```java
@Qualifier @Retention(RUNTIME) @interface Io {}
@Qualifier @Retention(RUNTIME) @interface Main {}

@Qualifier @Retention(RUNTIME)
@interface Tag { String value(); }        // 带成员：@Tag("v1") 与 @Tag("v2") 是两个 key

@Module static class M {
    @Provides @Io   static String io()   { return "IO"; }
    @Provides @Main static String main() { return "MAIN"; }
    @Provides @Tag("v1") static String v1() { return "V1"; }
    @Provides @Tag("v2") static String v2() { return "V2"; }
}
```

**断言**：五个同类型 `String` 绑定互不干扰（`IO/MAIN/CACHE/V1/V2`）；按 `@Io` 精确取值。

**坑**：限定符必须**和注入方成对**——`@Provides @Io String` 只满足 `@Io String`，满足不了裸 `String`。

### 4.2 `@Named`

**作用**：`@Named("x")` 本质是「带 `String value()` 成员的限定符」，Dagger 内置。

```java
@Provides @Named("cache") static String cache() { return "CACHE"; }
```

**断言**：`@Named` 自带 `@Qualifier`；`@Named("cache")` 精确匹配。

**建议**：`@Named` 能省一次建注解类，但**字符串不可重构、不可枚举**。跨模块共享的语义用自定义 `@Qualifier`，模块内部的参数才用 `@Named`。

---

## 5. 子组件

### 5.1 `@Subcomponent`

**作用**：父图的一棵子树。子图能用父图的所有绑定，**父图看不到子图**（单向）。

```java
@Subcomponent
interface FactoryChild {
    Dashboard dashboard();

    @Subcomponent.Factory
    interface Factory {
        FactoryChild create(@BindsInstance String owner);
    }
}
```

**断言**：子组件能拿到父图的 `Engine`；子组件自己的入参 `owner` 生效。

**为什么用子组件**：需要「比父图更短命的作用域」或「运行期入参」时。SystemUI 里 48 个 `@Subcomponent` 全是这个原因（每个 Keyguard Bouncer 一个）。

### 5.2 `@Subcomponent.Builder`

```java
@Subcomponent
interface BuilderChild {
    Dashboard dashboard();

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance Builder owner(String owner);
        BuilderChild build();
    }
}
```

**断言**：与 Factory 等价（`builder`）。

### 5.3 `@Module(subcomponents = ...)` —— 登记

**作用**：子组件必须被父图的某个 Module **登记**，否则父图里拿不到 `Xxx.Factory`。

```java
@Module(subcomponents = {FactoryChild.class, BuilderChild.class})
static class ParentMod {}

@Component(modules = ParentMod.class)
interface ParentCmp {
    FactoryChild.Factory factoryChild();
    BuilderChild.Builder builderChild();
}
```

**断言**：登记后工厂可注入；同一个父组件能开多个子组件实例。

**坑**：忘了登记 → `[Dagger/MissingBinding] FactoryChild.Factory cannot be provided`。

---

## 6. 多绑定（Set / Map）

### 6.1 `@IntoSet`

```java
@Module static class SetModA {
    @Provides @IntoSet static String a() { return "a"; }
}
```

**断言**：两个 Module 各贡献一个，合并进同一个 `Set`。

### 6.2 `@ElementsIntoSet`

```java
@Provides @ElementsIntoSet
static Set<String> bc() { return new HashSet<>(Arrays.asList("b", "c")); }
```

**断言**：一次贡献多个元素，和 `@IntoSet` 的贡献合并（最终 `a/b/c/d` 四条）。

### 6.3 `@IntoMap` + 内置 MapKey

```java
@Provides @IntoMap @StringKey("one") static Integer one() { return 1; }
@Provides @IntoMap @IntKey(2)       static Integer two() { return 2; }
@Provides @IntoMap @LongKey(3L)     static Integer three() { return 3; }
@Provides @IntoMap @ClassKey(String.class) static Integer forString() { return 4; }
```

| MapKey | 生成注入类型 |
|---|---|
| `@StringKey("one")` | `Map<String, Integer>` |
| `@IntKey(2)` | `Map<Integer, Integer>` |
| `@LongKey(3L)` | `Map<Long, Integer>` |
| `@ClassKey(String.class)` | `Map<Class<?>, Integer>` |

**断言**：四种 key 分别进四张不同的 Map。

### 6.4 `@MapKey` 自定义

```java
enum Color { RED, GREEN }

@MapKey @Retention(RUNTIME)
@interface ColorKey { Color value(); }     // 单成员 → unwrapValue 默认 true → key 类型 = Color

@Provides @IntoMap @ColorKey(Color.RED) static String red() { return "红"; }
```

**断言**：`Map<Color, String>` 里能取到 `RED → 红`。

### 6.5 `@Multibinds`

**作用**：声明「这个 Set/Map 可能是空的」，让 `Set<Long>` 这种没有任何贡献者的类型也能注入。

```java
@Module
abstract static class EmptyMod {
    @Multibinds abstract Set<Long> emptyLongs();
    @Multibinds abstract Map<String, Double> emptyDoubles();
}
```

**断言**：两个集合都能注入且为空。

**坑**：没有 `@Multibinds` 时注入空集合会 `[Dagger/MissingBinding]`；有了它，哪怕后来真的加了元素也不冲突。

---

## 7. Assisted 注入

### 7.1 `@AssistedInject` + `@Assisted`

**作用**：一部分依赖来自图（`Formatter`），一部分来自**调用现场**（`title`、`seq`）。传统做法要为此开子组件，Assisted 不用。

```java
static class Report {
    final String title; final int seq; final Formatter fmt;

    @AssistedInject
    Report(@Assisted String title, @Assisted int seq, Formatter fmt) { ... }

    @AssistedFactory
    interface Factory {
        Report create(String title, int seq);   // 只列 @Assisted 参数，顺序一致
    }
}
```

**断言**：`@Assisted` 参数按声明顺序注入（`#7 [月报]`）；图内依赖 `Formatter` 照常注入。

### 7.2 `@AssistedFactory`

**断言**：每次 `create` 都是新实例；`Formatter` 没加作用域 → 每个 `Report` 各一份（加了 `@Singleton` 才会共享）。

**坑**：
- `@AssistedFactory` 的工厂方法名可以随便，但**参数类型和顺序必须与 `@Assisted` 一致**。
- `@AssistedInject` 的类**不能同时**有 `@Inject` 构造。
- 工厂本身必须能被图提供（通常是嵌套在 `@AssistedInject` 类里，Dagger 自动生成实现）。

---

## 8. 常见编译期错误对照

| 错误码 | 触发场景 | 本次实际踩到的例子 | 解法 |
|---|---|---|---|
| `[Dagger/MissingBinding]` | 图里要的东西没人提供 | `Garage` 只有成员注入入口，却写成 `Garage garage();` | 改成 `void inject(Garage)` 或加 `@Inject` 构造 |
| `[Dagger/MissingBinding]` | 子组件没登记 | 没写 `@Module(subcomponents = X.class)` | 父图 Module 里登记 |
| `[Dagger/MissingBinding]` | 空集合没声明 | `Set<Long>` 直接注入 | 加 `@Multibinds` |
| `[Dagger/IncompatiblyScopedBindings]` | 作用域混用 | `@Singleton` 组件里引用 `@LocalScope` 的 `Session` | 拆成两个组件 |
| `[Dagger/DuplicateBindings]` | 同类型同限定符两条绑定 | 两个 Module 都 `@Provides String` | 加限定符或删一条 |
| `[Dagger/Nullable]` | `@Provides` 返回 `@Nullable` 但没标 | 想用 null 表示「没有」 | 改用 `@BindsOptionalOf` |
| 「`@Binds` must be abstract」 | `@Binds` 写了方法体 | — | 去掉方法体，Module 改 `abstract class` |

---

## 9. 不在本表里的注解

| 注解 | 为什么没放进来 |
|---|---|
| `@AutoAnnotation` | 来自 **Google Auto**（`com.google.auto.value:auto-annotation`），不是 Dagger；Dagger 只是能消费它生成的东西 |
| `@ContributesAndroidInjector` | 来自 `dagger-android`，已不推荐（Google 建议改用 `dagger.hilt` 或手写子组件） |
| `@HiltAndroidApp` / `@InstallIn` / `@AndroidEntryPoint` 等 | **Hilt** 的注解，不是 Dagger2 本身；SystemUI 也没用 Hilt |
| `@ProductionComponent` / `@Produces` / `@ProductionScope` | 生产模式（`dagger-producers` + Guava/RxJava），Android 上基本不用 |
| `@Component.Builder` 之外还有 `@Component.Factory` | 两者都已收录（#13、#14） |

---

## 10. 一句话记忆法

| 你想干什么 | 用什么 |
|---|---|
| 类是我写的 | `@Inject` 构造 |
| 类我改不了 | `@Provides` |
| 只是接口 → 实现 | `@Binds` |
| 需要「可能没有」 | `@BindsOptionalOf` |
| 值此刻才知道 | `@BindsInstance` + `Factory` |
| 同类型多份 | `@Qualifier` / `@Named` |
| 想要唯一实例 | `@Singleton` / 自定义 `@Scope` |
| 想要更短命 / 带运行期参数 | `@Subcomponent` |
| 需要集合 / 表 | `@IntoSet` / `@IntoMap` / `@Multibinds` |
| 图 + 现场混合 | `@AssistedInject` + `@AssistedFactory` |
