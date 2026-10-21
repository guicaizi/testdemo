package com.example.testdemo.dagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import dagger.Component;
import dagger.Module;
import dagger.Provides;

/**
 * 覆盖：@Qualifier（自定义）、@Named、带成员的限定符
 */
final class ItemQualifier {

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Io {
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Main {
    }

    /** 带成员的限定符：同一个注解类型，成员值不同就是不同 key */
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface Tag {
        String value();
    }

    static class Repo {
        final String all;

        @Inject
        Repo(@Io String io, @Main String main, @Named("cache") String cache,
                @Tag("v1") String v1, @Tag("v2") String v2) {
            this.all = io + "/" + main + "/" + cache + "/" + v1 + "/" + v2;
        }
    }

    @Module
    static class M {
        @Provides
        @Io
        static String io() {
            return "IO";
        }

        @Provides
        @Main
        static String main() {
            return "MAIN";
        }

        @Provides
        @Named("cache")
        static String cache() {
            return "CACHE";
        }

        @Provides
        @Tag("v1")
        static String v1() {
            return "V1";
        }

        @Provides
        @Tag("v2")
        static String v2() {
            return "V2";
        }
    }

    @Component(modules = M.class)
    interface Cmp {
        Repo repo();

        @Io
        String io();

        @Named("cache")
        String cache();
    }

    /** @Qualifier：给同类型多份绑定贴标签 */
    static List<T.Row> qualifier() {
        T t = new T();
        t.ok("@Qualifier 是元注解（Io 上标着 @Qualifier）",
                Io.class.isAnnotationPresent(Qualifier.class));
        Cmp cmp = DaggerItemQualifier_Cmp.create();
        t.eq("五个同类型 String 绑定互不干扰", "IO/MAIN/CACHE/V1/V2", cmp.repo().all);
        t.eq("按限定符取值：@Io", "IO", cmp.io());
        t.eq("带成员的限定符：@Tag(\"v1\") 与 @Tag(\"v2\") 是两个 key",
                "IO/MAIN/CACHE/V1/V2", cmp.repo().all);
        return t.rows();
    }

    /** @Named：字符串限定符（本质是 @Qualifier + String 成员） */
    static List<T.Row> named() {
        T t = new T();
        t.ok("@Named 自带 @Qualifier", Named.class.isAnnotationPresent(Qualifier.class));
        t.eq("@Named(\"cache\") 精确匹配", "CACHE",
                DaggerItemQualifier_Cmp.create().cache());
        return t.rows();
    }

    private ItemQualifier() {
    }
}
