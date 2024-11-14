package com.mido.libcompiler;

import com.mido.libannotation.PermissionDenied;
import com.mido.libannotation.PermissionGrant;
import com.mido.libannotation.PermissionRational;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class RunttimePermissionAbstractProcessor extends AbstractProcessor {
    private Elements mElementUtils;
    private Messager mMessager;
    private HashMap<String, MethodInfo> mMethodMap = new HashMap<>();
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mElementUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMethodMap.clear();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "processor start....");
        if (!handleAnnotationInfo(roundEnvironment, PermissionGrant.class)) {
            return false;
        }
        if (!handleAnnotationInfo(roundEnvironment, PermissionDenied.class)) {
            return false;
        }
        if (!handleAnnotationInfo(roundEnvironment, PermissionRational.class)) {
            return false;
        }
        for (String className: mMethodMap.keySet()) {
            MethodInfo methodInfo = mMethodMap.get(className);
            try {
                JavaFileObject sourceFile = mFiler.createSourceFile(methodInfo.mPackageName + "." + methodInfo.mFileName);
                Writer writer = sourceFile.openWriter();
                writer.write(methodInfo.generateJavaCode());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                mMessager.printMessage(Diagnostic.Kind.NOTE, "write file failed " + e.getMessage());
            }
            mMessager.printMessage(Diagnostic.Kind.NOTE, methodInfo.generateJavaCode());
        }
        mMessager.printMessage(Diagnostic.Kind.NOTE, "processor end....");
        return false;
    }

    private boolean handleAnnotationInfo(RoundEnvironment roundEnvironment, Class<? extends Annotation> annotation) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(annotation);
        for (Element element: elements) {
            if (!checkMethodValid(element, annotation)) {
                return false;
            }
            ExecutableElement executableElement = (ExecutableElement) element;
            TypeElement enclosingElement = (TypeElement) executableElement.getEnclosingElement();
            String className = enclosingElement.getQualifiedName().toString();

            MethodInfo methodInfo = mMethodMap.get(className);
            if (methodInfo == null) {
                methodInfo = new MethodInfo(mElementUtils, enclosingElement, mMessager);
                mMethodMap.put(className, methodInfo);
            }

            Annotation annotationClass = executableElement.getAnnotation(annotation);
            String methodName = executableElement.getSimpleName().toString();
            List<? extends VariableElement> parameters = executableElement.getParameters();

            if (parameters == null || parameters.isEmpty()) {
                String message = "the method %s marked by annotation %s must have an unique parameter";
                throw new IllegalArgumentException(String.format(
                        message, methodName, annotationClass.getClass().getSimpleName()));
            }

            if (annotationClass instanceof PermissionGrant) {
                int requestCode = ((PermissionGrant) annotationClass).value();
                methodInfo.grantMethodMap.put(requestCode, methodName);
            } else if (annotationClass instanceof PermissionDenied) {
                int requestCode = ((PermissionDenied) annotationClass).value();
                methodInfo.deniedMethodMap.put(requestCode, methodName);
            } else if (annotationClass instanceof PermissionRational) {
                int requestCode = ((PermissionRational) annotationClass).value();
                methodInfo.rationalMethodMap.put(requestCode, methodName);
            }
        }
        return true;
    }

    private boolean checkMethodValid(Element element, Class<? extends Annotation> annotation) {
        if (element.getKind() != ElementKind.METHOD) {
            return false;
        }
        if (ClassValidator.isPrivate(element) || ClassValidator.isAbstract(element)) {
            return false;
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportList = new HashSet<>();
        supportList.add(PermissionGrant.class.getCanonicalName());
        supportList.add(PermissionDenied.class.getCanonicalName());
        supportList.add(PermissionRational.class.getCanonicalName());
        return supportList;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
