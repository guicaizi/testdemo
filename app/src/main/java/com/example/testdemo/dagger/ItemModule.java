package com.example.testdemo.dagger;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Binds;
import dagger.BindsInstance;
import dagger.BindsOptionalOf;
import dagger.Component;
import dagger.Module;
import dagger.Provides;

/**
 * 覆盖：@Module、@Provides、@Binds、@BindsOptionalOf、@BindsInstance、
 * @Component.Factory、@Component.Builder、@Component(dependencies = ...)
 */
final class ItemModule {

    // ================= @Module / @Provides / includes =================

    @Module
    static class ConfigMod {
        @Provides
        static String prefix() {
            return "v";
        }

        @Provides
        @Named("version")
        static String version(String prefix) {          // @Provides 可以带逻辑、可以依赖别的绑定
            return prefix + "1.0.0";
        }
    }

    @Module(includes = ConfigMod.class)                 // 合并另一个 Module 的绑定
    static class AppMod {
        @Provides
        static Integer retry() {
            return 3;
        }
    }

    static class Banner {
        final String version;
        final Integer retry;

        @Inject
        Banner(@Named("version") String version, Integer retry) {
            this.version = version;
            this.retry = retry;
        }
    }

    @Component(modules = AppMod.class)
    interface ModuleCmp {
        Banner banner();
    }

    // ================= @Binds =================

    interface Svc {
        String name();
    }

    static class SvcImpl implements Svc {
        @Inject
        SvcImpl() {
        }

        @Override
        public String name() {
            return "impl";
        }
    }

    @Module
    abstract static class BindsMod {
        @Binds
        abstract Svc bindSvc(SvcImpl impl);             // 只做类型映射，不 new
    }

    @Component(modules = BindsMod.class)
    interface BindsCmp {
        Svc svc();
    }

    // ================= @BindsOptionalOf =================

    @Module
    abstract static class OptMod {
        @BindsOptionalOf
        abstract Svc optionalSvc();                      // 声明「可能有」，不提供实现
    }

    @Component(modules = OptMod.class)
    interface OptAbsentCmp {
        Optional<Svc> svc();
    }

    @Component(modules = {OptMod.class, BindsMod.class})
    interface OptPresentCmp {
        Optional<Svc> svc();
    }

    // ================= @BindsInstance + @Component.Factory =================

    static class Welcome {
        final String user;

        @Inject
        Welcome(String user) {
            this.user = user;
        }
    }

    @Component
    interface FactoryCmp {
        Welcome welcome();

        @Component.Factory
        interface Factory {
            FactoryCmp create(@BindsInstance String user);
        }
    }

    // ================= @Component.Builder =================

    @Component
    interface BuilderCmp {
        Welcome welcome();

        @Component.Builder
        interface Builder {
            @BindsInstance
            Builder user(String user);

            BuilderCmp build();
        }
    }

    // ================= @Component(dependencies = ...) =================

    interface Clock {
        long now();
    }

    static class Timer {
        final Clock clock;

        @Inject
        Timer(Clock clock) {
            this.clock = clock;
        }
    }

    @Module
    static class ClockMod {
        @Provides
        static Clock clock() {
            return new Clock() {
                @Override
                public long now() {
                    return 42L;
                }
            };
        }
    }

    @Component(modules = ClockMod.class)
    interface DepCmp {
        Clock clock();                                   // 必须显式暴露，才能被别的组件用到
    }

    @Component(dependencies = DepCmp.class)
    interface DependentCmp {
        Timer timer();
    }

    // ================= 测试项 =================

    /** @Module + @Provides + includes */
    static List<T.Row> module() {
        T t = new T();
        Banner b = DaggerItemModule_ModuleCmp.create().banner();
        t.eq("@Provides 里的逻辑被执行（prefix+1.0.0）", "v1.0.0", b.version);
        t.eq("@Module(includes) 把另一个 Module 的绑定合并进来了", 3, b.retry);
        return t.rows();
    }

    /** @Provides：手写工厂 */
    static List<T.Row> provides() {
        T t = new T();
        t.ok("@Provides 适合「类改不了 / 需要装配逻辑 / 返回值来自三方」的场景", true);
        t.eq("@Provides 方法参数可以依赖其他绑定（String prefix → version）",
                "v1.0.0", DaggerItemModule_ModuleCmp.create().banner().version);
        return t.rows();
    }

    /** @Binds：接口 → 实现 */
    static List<T.Row> binds() {
        T t = new T();
        Svc svc = DaggerItemModule_BindsCmp.create().svc();
        t.eq("注入方拿到的是实现类的行为", "impl", svc.name());
        t.ok("@Binds 返回接口类型、参数是实现类型", svc instanceof SvcImpl);
        return t.rows();
    }

    /** @BindsOptionalOf：可选绑定 */
    static List<T.Row> bindsOptional() {
        T t = new T();
        Optional<Svc> absent = DaggerItemModule_OptAbsentCmp.create().svc();
        Optional<Svc> present = DaggerItemModule_OptPresentCmp.create().svc();
        t.ok("没有实现时 = Optional.empty()（不会编译失败）", !absent.isPresent());
        t.ok("有实现时 = Optional.of(impl)",
                present.isPresent() && "impl".equals(present.get().name()));
        return t.rows();
    }

    /** @BindsInstance：把运行期值塞进图 */
    static List<T.Row> bindsInstance() {
        T t = new T();
        Welcome w = DaggerItemModule_FactoryCmp.factory().create("alice").welcome();
        t.eq("运行期传参进入依赖图", "alice", w.user);
        return t.rows();
    }

    /** @Component.Factory：推荐的无参组件工厂 */
    static List<T.Row> componentFactory() {
        T t = new T();
        FactoryCmp.Factory f = DaggerItemModule_FactoryCmp.factory();
        t.ok("factory() 返回 @Component.Factory 实例", f != null);
        t.ok("create(...) 的每个参数都必须标 @BindsInstance",
                f.create("bob").welcome() != null);
        return t.rows();
    }

    /** @Component.Builder：旧的 builder 写法 */
    static List<T.Row> componentBuilder() {
        T t = new T();
        BuilderCmp c = DaggerItemModule_BuilderCmp.builder().user("carol").build();
        t.eq("@Component.Builder 与 Factory 等价", "carol", c.welcome().user);
        return t.rows();
    }

    /** @Component(dependencies = ...)：把外部的图接进来 */
    static List<T.Row> componentDependencies() {
        T t = new T();
        DepCmp dep = DaggerItemModule_DepCmp.create();
        DependentCmp child = DaggerItemModule_DependentCmp.builder().depCmp(dep).build();
        t.eq("依赖组件暴露的 Clock 被复用", 42L, child.timer().clock.now());
        t.ok("dependencies 只传递暴露出来的类型（DepCmp.clock()）", dep.clock() != null);
        return t.rows();
    }

    private ItemModule() {
    }
}
