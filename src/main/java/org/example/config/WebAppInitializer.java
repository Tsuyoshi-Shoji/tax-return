package org.example.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer
        extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        // 今回はルートコンテキスト未使用
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        // Spring MVC の設定クラス
        return new Class<?>[]{ WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        // 全リクエストを DispatcherServlet が処理
        return new String[]{ "/" };
    }
}
