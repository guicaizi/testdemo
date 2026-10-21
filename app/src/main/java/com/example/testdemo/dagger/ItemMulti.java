package com.example.testdemo.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dagger.Component;
import dagger.MapKey;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.IntKey;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import dagger.multibindings.LongKey;
import dagger.multibindings.Multibinds;
import dagger.multibindings.StringKey;

/**
 * 覆盖：@IntoSet、@ElementsIntoSet、@IntoMap、@StringKey、@IntKey、@LongKey、@ClassKey、
 * @MapKey（自定义）、@Multibinds
 */
final class ItemMulti {

    // ================= Set 多绑定 =================

    @Module
    static class SetModA {
        @Provides
        @IntoSet
        static String a() {
            return "a";
        }
    }

    @Module
    static class SetModB {
        @Provides
        @IntoSet
        static String d() {
            return "d";
        }

        @Provides
        @ElementsIntoSet
        static Set<String> bc() {                 // 一次贡献多个元素
            return new HashSet<>(Arrays.asList("b", "c"));
        }
    }

    @Component(modules = {SetModA.class, SetModB.class})
    interface SetCmp {
        Set<String> strings();
    }

    // ================= Map 多绑定 =================

    @Module
    static class MapMod {
        @Provides
        @IntoMap
        @StringKey("one")
        static Integer one() {
            return 1;
        }

        @Provides
        @IntoMap
        @IntKey(2)
        static Integer two() {
            return 2;
        }

        @Provides
        @IntoMap
        @LongKey(3L)
        static Integer three() {
            return 3;
        }

        @Provides
        @IntoMap
        @ClassKey(String.class)
        static Integer forString() {
            return 4;
        }
    }

    // ================= 自定义 @MapKey =================

    enum Color {RED, GREEN}

    @MapKey
    @Retention(RetentionPolicy.RUNTIME)
    @interface ColorKey {
        Color value();                            // 单成员 → unwrapValue 默认 true → key 类型是 Color
    }

    @Module
    static class ColorMod {
        @Provides
        @IntoMap
        @ColorKey(Color.RED)
        static String red() {
            return "红";
        }

        @Provides
        @IntoMap
        @ColorKey(Color.GREEN)
        static String green() {
            return "绿";
        }
    }

    // ================= @Multibinds：声明「可能为空」 =================

    @Module
    abstract static class EmptyMod {
        @Multibinds
        abstract Set<Long> emptyLongs();

        @Multibinds
        abstract Map<String, Double> emptyDoubles();
    }

    @Component(modules = {MapMod.class, ColorMod.class, EmptyMod.class})
    interface MapCmp {
        Map<String, Integer> byString();

        Map<Integer, Integer> byInt();

        Map<Long, Integer> byLong();

        Map<Class<?>, Integer> byClass();

        Map<Color, String> byColor();

        Set<Long> emptyLongs();

        Map<String, Double> emptyDoubles();
    }

    // ================= 测试项 =================

    /** @IntoSet */
    static List<T.Row> intoSet() {
        T t = new T();
        Set<String> set = DaggerItemMulti_SetCmp.create().strings();
        t.eq("@IntoSet 与 @ElementsIntoSet 合并到同一个 Set", 4, set.size());
        t.ok("两个 Module 的贡献都在", set.containsAll(Arrays.asList("a", "d")));
        return t.rows();
    }

    /** @ElementsIntoSet */
    static List<T.Row> elementsIntoSet() {
        T t = new T();
        Set<String> set = DaggerItemMulti_SetCmp.create().strings();
        t.ok("一次贡献多个元素（b、c）",
                set.containsAll(Arrays.asList("b", "c")));
        return t.rows();
    }

    /** @IntoMap */
    static List<T.Row> intoMap() {
        T t = new T();
        MapCmp cmp = DaggerItemMulti_MapCmp.create();
        t.eq("@IntoMap + @StringKey", 1, cmp.byString().get("one"));
        t.ok("每个 key 都进同一张 Map", cmp.byString().size() == 1);
        return t.rows();
    }

    /** @StringKey / @IntKey / @LongKey / @ClassKey */
    static List<T.Row> builtInKeys() {
        T t = new T();
        MapCmp cmp = DaggerItemMulti_MapCmp.create();
        t.eq("@IntKey → Map<Integer,Integer>", 2, cmp.byInt().get(2));
        t.eq("@LongKey → Map<Long,Integer>", 3, cmp.byLong().get(3L));
        t.eq("@ClassKey → Map<Class<?>,Integer>", 4, cmp.byClass().get(String.class));
        t.ok("不同 key 类型 = 不同的 Map", cmp.byString().size() == 1
                && cmp.byInt().size() == 1);
        return t.rows();
    }

    /** @MapKey 自定义 */
    static List<T.Row> mapKey() {
        T t = new T();
        Map<Color, String> m = DaggerItemMulti_MapCmp.create().byColor();
        t.eq("自定义 @MapKey：key 类型 = 枚举", "红", m.get(Color.RED));
        t.eq("另一个 key", "绿", m.get(Color.GREEN));
        return t.rows();
    }

    /** @Multibinds */
    static List<T.Row> multibinds() {
        T t = new T();
        MapCmp cmp = DaggerItemMulti_MapCmp.create();
        t.ok("@Multibinds 让空 Set 也能注入", cmp.emptyLongs().isEmpty());
        t.ok("@Multibinds 让空 Map 也能注入", cmp.emptyDoubles().isEmpty());
        return t.rows();
    }

    private ItemMulti() {
    }
}
