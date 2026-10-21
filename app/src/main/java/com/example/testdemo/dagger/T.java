package com.example.testdemo.dagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 极简断言记录器 —— 每个测试项返回一组 Row，由界面渲染 PASS/FAIL。
 *
 * <p>刻意不依赖 JUnit：这些用例要在手机上点一下就出结果，不需要跑 instrumentation。
 */
public final class T {

    /** 一行断言结果 */
    public static final class Row {
        public final String name;
        public final boolean pass;
        public final String detail;

        Row(String name, boolean pass, String detail) {
            this.name = name;
            this.pass = pass;
            this.detail = detail;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public void ok(String name, boolean cond) {
        rows.add(new Row(name, cond, null));
    }

    public void eq(String name, Object expected, Object actual) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        String detail = same ? null : ("期望=" + expected + "，实际=" + actual);
        rows.add(new Row(name, same, detail));
    }

    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }
}
