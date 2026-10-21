package com.example.testdemo.dagger;

import java.util.List;

import javax.inject.Inject;

import dagger.Component;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * 覆盖：@AssistedInject、@Assisted、@AssistedFactory
 *
 * <p>一句话：一部分依赖来自图，另一部分来自「调用现场」，用 Assisted 就不必为此开子组件。
 */
final class ItemAssisted {

    static class Formatter {
        @Inject
        Formatter() {
        }

        String wrap(String raw) {
            return "[" + raw + "]";
        }
    }

    static class Report {
        final String title;      // 现场给的
        final int seq;           // 现场给的
        final Formatter fmt;     // 图里给的

        @AssistedInject
        Report(@Assisted String title, @Assisted int seq, Formatter fmt) {
            this.title = title;
            this.seq = seq;
            this.fmt = fmt;
        }

        String render() {
            return "#" + seq + " " + fmt.wrap(title);
        }

        @AssistedFactory
        interface Factory {
            Report create(String title, int seq);   // 只列 @Assisted 参数，顺序一致
        }
    }

    @Component
    interface Cmp {
        Report.Factory reportFactory();
    }

    /** @AssistedInject + @Assisted */
    static List<T.Row> assistedInject() {
        T t = new T();
        Report.Factory f = DaggerItemAssisted_Cmp.create().reportFactory();
        Report r = f.create("月报", 7);
        t.eq("@Assisted 参数按声明顺序注入", "#7 [月报]", r.render());
        t.ok("图内依赖（Formatter）照常注入", r.fmt != null);
        return t.rows();
    }

    /** @AssistedFactory */
    static List<T.Row> assistedFactory() {
        T t = new T();
        Cmp cmp = DaggerItemAssisted_Cmp.create();
        Report a = cmp.reportFactory().create("A", 1);
        Report b = cmp.reportFactory().create("B", 2);
        t.ok("每次 create 都是新实例", a != b);
        // Formatter 没加作用域，所以每次 create 都会新建一个；想共享就给 Formatter 加 @Singleton
        t.ok("Formatter 未加作用域 → 每个 Report 各一份", a.fmt != b.fmt);
        t.eq("两个实例的参数互不干扰", "#1 [A]", a.render());
        return t.rows();
    }

    private ItemAssisted() {
    }
}
