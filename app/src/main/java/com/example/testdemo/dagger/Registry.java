package com.example.testdemo.dagger;

import java.util.ArrayList;
import java.util.List;

/**
 * Dagger2 注解测试清单。
 *
 * <p>界面按这里的顺序渲染，每项独立可跑；一项失败不影响其他项。
 */
public final class Registry {

    /** 一个测试项的一次执行 */
    public interface Body {
        List<T.Row> run();
    }

    /** 列表里的一行 */
    public static final class Item {
        /** 注解名，例如 "@Inject"、"@IntoMap" */
        public final String ann;
        /** 这一项测什么 */
        public final String title;
        /** 一句话解释 */
        public final String summary;

        private final Body body;

        Item(String ann, String title, String summary, Body body) {
            this.ann = ann;
            this.title = title;
            this.summary = summary;
            this.body = body;
        }

        public List<T.Row> run() {
            return body.run();
        }
    }

    public static List<Item> all() {
        List<Item> list = new ArrayList<>(32);

        // ---------- 注入与作用域 ----------
        list.add(new Item("@Inject", "构造注入",
                "构造参数就是依赖时，Dagger 直接 new 并传参",
                ItemInject::injectConstructor));
        list.add(new Item("@Inject", "字段 / 方法注入",
                "没有构造注入的类（Activity、View）靠成员注入",
                ItemInject::injectMembers));
        list.add(new Item("@Component", "整张图的入口",
                "create() 建图；接口方法就是图的出口",
                ItemInject::component));
        list.add(new Item("@Singleton", "组件内唯一",
                "作用域 = 在组件生命周期内缓存同一个实例",
                ItemInject::singleton));
        list.add(new Item("@Scope", "自定义作用域",
                "自己声明一个作用域，语义同 @Singleton，只是名字不同",
                ItemInject::scope));
        list.add(new Item("@Reusable", "可复用",
                "不保证同一实例，适合「建起来贵但不要求唯一」的对象",
                ItemInject::reusable));
        list.add(new Item("dagger.Lazy", "延迟 + 缓存",
                "第一次 get() 才创建，之后复用同一个",
                ItemInject::lazy));
        list.add(new Item("Provider<T>", "每次新建",
                "需要多个实例、或想延迟到用时再取，用 Provider",
                ItemInject::provider));

        // ---------- 绑定方式 ----------
        list.add(new Item("@Module", "绑定容器",
                "includes 可以把多个 Module 合成一张绑定表",
                ItemModule::module));
        list.add(new Item("@Provides", "手写工厂",
                "类改不了、或建对象时要顺便做装配时用",
                ItemModule::provides));
        list.add(new Item("@Binds", "接口别名",
                "零成本：不 new、不写方法体，只做类型映射",
                ItemModule::binds));
        list.add(new Item("@BindsOptionalOf", "可选能力",
                "设备/形态可能没有的能力，用 Optional 表达",
                ItemModule::bindsOptional));
        list.add(new Item("@BindsInstance", "运行期入参",
                "把此刻才知道的值（用户、Intent 参数）塞进图",
                ItemModule::bindsInstance));
        list.add(new Item("@Component.Factory", "组件工厂",
                "推荐的无参组件构建方式，@BindsInstance 作参数",
                ItemModule::componentFactory));
        list.add(new Item("@Component.Builder", "组件 Builder",
                "旧写法，逐项 set 后 build，与 Factory 等价",
                ItemModule::componentBuilder));
        list.add(new Item("@Component(dependencies)", "组件依赖",
                "只传递被依赖组件暴露出来的类型，不是继承",
                ItemModule::componentDependencies));

        // ---------- 限定符 ----------
        list.add(new Item("@Qualifier", "自定义限定符",
                "同一个类型有多份绑定时，用注解把它们区分开",
                ItemQualifier::qualifier));
        list.add(new Item("@Named", "字符串限定符",
                "本质是 @Qualifier + String，能不用就不用",
                ItemQualifier::named));

        // ---------- 子组件 ----------
        list.add(new Item("@Subcomponent", "子组件",
                "子图能用父图的绑定，反向不行（单向继承）",
                ItemSubcomponent::subcomponent));
        list.add(new Item("@Subcomponent.Factory", "子组件工厂",
                "创建点由父图提供，可带 @BindsInstance 参数",
                ItemSubcomponent::subcomponentFactory));
        list.add(new Item("@Subcomponent.Builder", "子组件 Builder",
                "与 Factory 等价的旧写法",
                ItemSubcomponent::subcomponentBuilder));
        list.add(new Item("@Module(subcomponents)", "登记子组件",
                "不在这里登记，子组件工厂就不可注入",
                ItemSubcomponent::moduleSubcomponents));

        // ---------- 多绑定 ----------
        list.add(new Item("@IntoSet", "集合多绑定",
                "多个 Module 往同一个 Set 里加元素",
                ItemMulti::intoSet));
        list.add(new Item("@ElementsIntoSet", "批量入集合",
                "一次贡献多个元素（Set / List）",
                ItemMulti::elementsIntoSet));
        list.add(new Item("@IntoMap", "表多绑定",
                "往同一张 Map 里加键值对，key 由 MapKey 注解决定",
                ItemMulti::intoMap));
        list.add(new Item("@StringKey / @IntKey / @LongKey / @ClassKey", "内置 MapKey",
                "key 的类型决定最终注入到的 Map 类型",
                ItemMulti::builtInKeys));
        list.add(new Item("@MapKey", "自定义 MapKey",
                "单成员注解，key 类型就是该成员的类型",
                ItemMulti::mapKey));
        list.add(new Item("@Multibinds", "空集合声明",
                "允许 Set / Map 一个元素都没有也能注入",
                ItemMulti::multibinds));

        // ---------- Assisted ----------
        list.add(new Item("@AssistedInject / @Assisted", "现场参数",
                "一部分依赖来自图，一部分来自调用现场",
                ItemAssisted::assistedInject));
        list.add(new Item("@AssistedFactory", "辅助工厂",
                "只声明 @Assisted 参数，顺序与构造一致",
                ItemAssisted::assistedFactory));

        return list;
    }

    private Registry() {
    }
}
