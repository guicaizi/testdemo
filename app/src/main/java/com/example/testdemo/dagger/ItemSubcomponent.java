package com.example.testdemo.dagger;

import java.util.List;

import javax.inject.Inject;

import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;
import dagger.Subcomponent;

/**
 * 覆盖：@Subcomponent、@Subcomponent.Factory、@Subcomponent.Builder、@Module(subcomponents = ...)
 */
final class ItemSubcomponent {

    /** 父图的绑定，子图可以直接用 */
    static class Engine {
        @Inject
        Engine() {
        }
    }

    static class Dashboard {
        final String owner;
        final Engine engine;

        @Inject
        Dashboard(String owner, Engine engine) {
            this.owner = owner;
            this.engine = engine;
        }
    }

    // ================= @Subcomponent + @Subcomponent.Factory =================

    @Subcomponent
    interface FactoryChild {
        Dashboard dashboard();

        @Subcomponent.Factory
        interface Factory {
            FactoryChild create(@BindsInstance String owner);
        }
    }

    // ================= @Subcomponent + @Subcomponent.Builder =================

    @Subcomponent
    interface BuilderChild {
        Dashboard dashboard();

        @Subcomponent.Builder
        interface Builder {
            @BindsInstance
            Builder owner(String owner);

            BuilderChild build();
        }
    }

    /**
     * @Module(subcomponents = ...)：把子组件「登记」到父图。
     * 没这一句，DaggerItemSubcomponent_ParentCmp 里就没有 FactoryChild.Factory 可注入。
     */
    @Module(subcomponents = {FactoryChild.class, BuilderChild.class})
    static class ParentMod {
    }

    @Component(modules = ParentMod.class)
    interface ParentCmp {
        FactoryChild.Factory factoryChild();

        BuilderChild.Builder builderChild();
    }

    /** @Subcomponent：父图能注入给子图，反过来不行 */
    static List<T.Row> subcomponent() {
        T t = new T();
        ParentCmp parent = DaggerItemSubcomponent_ParentCmp.create();
        Dashboard d = parent.factoryChild().create("factory").dashboard();
        t.ok("子组件能拿到父图的 Engine", d.engine != null);
        t.eq("子组件自己的入参 owner", "factory", d.owner);
        return t.rows();
    }

    /** @Subcomponent.Factory */
    static List<T.Row> subcomponentFactory() {
        T t = new T();
        FactoryChild.Factory f = DaggerItemSubcomponent_ParentCmp.create().factoryChild();
        t.ok("父图暴露子组件工厂", f != null);
        t.ok("工厂 create(...) 里可以带 @BindsInstance",
                f.create("x").dashboard() != null);
        return t.rows();
    }

    /** @Subcomponent.Builder */
    static List<T.Row> subcomponentBuilder() {
        T t = new T();
        Dashboard d = DaggerItemSubcomponent_ParentCmp.create()
                .builderChild().owner("builder").build().dashboard();
        t.eq("Builder 写法与 Factory 等价", "builder", d.owner);
        return t.rows();
    }

    /** @Module(subcomponents = ...) */
    static List<T.Row> moduleSubcomponents() {
        T t = new T();
        ParentCmp parent = DaggerItemSubcomponent_ParentCmp.create();
        t.ok("Module 里登记后，子组件工厂才能注入", parent.factoryChild() != null);
        t.ok("同一个父组件可以开多个子组件实例",
                parent.factoryChild().create("a") != parent.factoryChild().create("b"));
        return t.rows();
    }

    private ItemSubcomponent() {
    }
}
