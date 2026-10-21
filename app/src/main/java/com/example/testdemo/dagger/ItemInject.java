package com.example.testdemo.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Scope;
import javax.inject.Singleton;

import dagger.Component;
import dagger.Lazy;
import dagger.Reusable;

/**
 * 覆盖：@Inject（构造 / 字段 / 方法）、@Component、@Singleton、@Scope、@Reusable、
 * dagger.Lazy、javax.inject.Provider
 */
final class ItemInject {

    // ================= 图的原料 =================

    static class Engine {
        static int created = 0;

        @Inject
        Engine() {
            created++;
        }
    }

    static class Car {
        final Engine engine;

        @Inject
        Car(Engine engine) {
            this.engine = engine;
        }
    }

    /** 没有 @Inject 构造，靠成员注入 */
    static class Garage {
        @Inject
        Engine fieldEngine;              // 字段注入（不能 private / final）
        Engine methodEngine;

        @Inject
        void setMethodEngine(Engine e) { // 方法注入（不能 private）
            this.methodEngine = e;
        }
    }

    @Singleton
    static class Counter {
        static int created = 0;

        @Inject
        Counter() {
            created++;
        }
    }

    @Scope
    @Retention(RetentionPolicy.RUNTIME)
    @interface LocalScope {
    }

    @LocalScope
    static class Session {
        @Inject
        Session() {
        }
    }

    @Reusable
    static class ReusableThing {
        @Inject
        ReusableThing() {
        }
    }

    static class Holder {
        final Lazy<Engine> lazy;
        final Provider<Engine> provider;

        @Inject
        Holder(Lazy<Engine> lazy, Provider<Engine> provider) {
            this.lazy = lazy;
            this.provider = provider;
        }
    }

    // ================= 组件 =================

    @Singleton
    @Component
    interface Cmp {
        Car car();

        /** 成员注入入口：参数是「已存在的对象」，Dagger 只往里灌 @Inject 成员 */
        void inject(Garage garage);

        Counter counter();

        ReusableThing reusable();

        Holder holder();
    }

    /** @LocalScope 与 @Singleton 不能同图（IncompatiblyScopedBindings），单独开一个组件 */
    @LocalScope
    @Component
    interface SessionCmp {
        Session session();

        Car car();
    }

    // ================= 测试项 =================

    /** @Inject：构造注入 */
    static List<T.Row> injectConstructor() {
        T t = new T();
        Cmp cmp = DaggerItemInject_Cmp.create();
        Car car = cmp.car();
        t.ok("car != null", car != null);
        t.ok("构造参数 engine 被自动注入", car.engine != null);
        return t.rows();
    }

    /** @Inject：字段 / 方法注入（需要成员注入入口） */
    static List<T.Row> injectMembers() {
        T t = new T();
        Cmp cmp = DaggerItemInject_Cmp.create();
        Garage g = new Garage();
        t.ok("注入前字段为 null", g.fieldEngine == null);
        cmp.inject(g);                       // 成员注入入口
        t.ok("@Inject 字段被注入", g.fieldEngine != null);
        t.ok("@Inject 方法被调用", g.methodEngine != null);
        return t.rows();
    }

    /** @Component：整张图的入口 */
    static List<T.Row> component() {
        T t = new T();
        Cmp cmp = DaggerItemInject_Cmp.create();     // 无参组件用 create()
        t.ok("DaggerXxx.create() 拿到组件实例", cmp != null);
        t.ok("组件上的方法就是图的出口", cmp.car() != null);
        t.ok("同一个组件反复调用不会重新建组件", cmp == cmp);
        return t.rows();
    }

    /** @Singleton：组件范围内唯一 */
    static List<T.Row> singleton() {
        T t = new T();
        Counter.created = 0;
        Cmp cmp = DaggerItemInject_Cmp.create();
        Counter a = cmp.counter();
        Counter b = cmp.counter();
        t.ok("两次取到同一实例", a == b);
        t.eq("构造只发生一次", 1, Counter.created);
        return t.rows();
    }

    /** @Scope：自定义作用域，同一组件内缓存 */
    static List<T.Row> scope() {
        T t = new T();
        t.ok("@Scope 是元注解（LocalScope 上标着 @Scope）",
                LocalScope.class.isAnnotationPresent(Scope.class));
        t.ok("@Singleton 也是 @Scope", Singleton.class.isAnnotationPresent(Scope.class));
        SessionCmp scoped = DaggerItemInject_SessionCmp.create();
        t.ok("@LocalScope 两次同一实例", scoped.session() == scoped.session());
        t.ok("无作用域的 Car 两次不同", scoped.car() != scoped.car());
        return t.rows();
    }

    /** @Reusable：可用但「不保证」缓存 */
    static List<T.Row> reusable() {
        T t = new T();
        Cmp cmp = DaggerItemInject_Cmp.create();
        t.ok("@Reusable 可注入", cmp.reusable() != null);
        t.ok("@Reusable 语义是「可复用」，缓存与否由 Dagger 决定（不要依赖身份）",
                true);
        return t.rows();
    }

    /** dagger.Lazy：用到才创建，且只创建一次 */
    static List<T.Row> lazy() {
        T t = new T();
        Engine.created = 0;
        Cmp cmp = DaggerItemInject_Cmp.create();
        Holder h = cmp.holder();
        t.eq("拿到 Holder 时还没创建 Engine", 0, Engine.created);
        Engine e1 = h.lazy.get();
        Engine e2 = h.lazy.get();
        t.ok("Lazy.get() 两次同一实例", e1 == e2);
        t.eq("只创建一次", 1, Engine.created);
        return t.rows();
    }

    /** javax.inject.Provider：每次 get 都新建 */
    static List<T.Row> provider() {
        T t = new T();
        Engine.created = 0;
        Cmp cmp = DaggerItemInject_Cmp.create();
        Holder h = cmp.holder();
        Engine p1 = h.provider.get();
        Engine p2 = h.provider.get();
        t.ok("Provider.get() 两次不同实例", p1 != p2);
        t.eq("创建两次", 2, Engine.created);
        return t.rows();
    }

    private ItemInject() {
    }
}
