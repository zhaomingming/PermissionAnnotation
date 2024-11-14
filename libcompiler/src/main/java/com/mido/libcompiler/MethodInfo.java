package com.mido.libcompiler;

import java.util.HashMap;

import javax.annotation.processing.Messager;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class MethodInfo {
    public String mClassName;
    public String mPackageName;
    public String mFileName;
    private Messager mMessager;
    public HashMap<Integer, String> grantMethodMap = new HashMap<>();
    public HashMap<Integer, String> deniedMethodMap = new HashMap<>();
    public HashMap<Integer, String> rationalMethodMap = new HashMap<>();

    private static final String PROXY_NAME = "PermissionProxy";

    public MethodInfo(Elements elements, TypeElement typeElement, Messager messager) {
        PackageElement packageElement = elements.getPackageOf(typeElement);
        mPackageName = packageElement.getQualifiedName().toString();
        mClassName = ClassValidator.getClassName(typeElement, mPackageName);
        mFileName = mClassName + "$$" + PROXY_NAME;
        mMessager = messager;
        mMessager.printMessage(Diagnostic.Kind.NOTE, "className: " + mClassName);
    }

    public String generateJavaCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("//generate code. Do not modify\n");
        builder.append("package ").append(mPackageName).append(";\n\n");
        builder.append("import com.mido.libpermissionhelper.*;");
        builder.append("\n");
        builder.append("public class ").append(mFileName)
                .append(" implements " + PROXY_NAME + "<").append(mClassName).append(">");
        builder.append("{\n");
        generateMethods(builder);
        builder.append("}\n");
        return builder.toString();
    }

    private void generateMethods(StringBuilder builder) {
        generateGrantMethod(builder);
        generateDeniedMethod(builder);
        generateRationalMethod(builder);
    }

    private void generateRationalMethod(StringBuilder builder) {
        builder.append("@Override\n");
        builder.append("public boolean rational(int requestCode, ")
                .append(mClassName).append(" source, String[] permissions," +
                        " PermissionRationalCallback permissionRationalCallback) {\n");
        builder.append("switch(requestCode){");
        for (int requestCode: rationalMethodMap.keySet()) {
            builder.append("case ").append(requestCode).append(":");
            builder.append("source.").append(rationalMethodMap.get(requestCode))
                    .append("(permissions, permissionRationalCallback);");
            builder.append("return true;");
        }
        builder.append(" }\n");
        builder.append("return false;");
        builder.append("}");
    }

    private void generateDeniedMethod(StringBuilder builder) {
        builder.append("@Override\n");
        builder.append("public void denied(int requestCode, ")
                .append(mClassName).append(" source, String[] permissions) {\n");
        builder.append("switch(requestCode){");
        for (int requestCode: deniedMethodMap.keySet()) {
            builder.append("case ").append(requestCode).append(":");
            builder.append("source.").append(deniedMethodMap.get(requestCode))
                    .append("(permissions);");
            builder.append("break;");
        }
        builder.append("}");
        builder.append(" }\n");
    }

    private void generateGrantMethod(StringBuilder builder) {
        builder.append("@Override\n");
        builder.append("public void grant(int requestCode, ").append(mClassName)
                .append(" source, String[] permissions) {\n");
        builder.append("switch(requestCode){");
        for (int requestCode: grantMethodMap.keySet()) {
            builder.append("case ").append(requestCode).append(":");
            builder.append("source.").append(grantMethodMap.get(requestCode))
                    .append("(permissions);");
            builder.append("break;");
        }
        builder.append("}");
        builder.append(" }\n");
    }
}
