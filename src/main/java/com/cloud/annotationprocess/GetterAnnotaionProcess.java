package com.cloud.annotationprocess;

import com.cloud.annotation.Getter;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * @author bin.yin
 * @version 1.0
 * @createTime 2019/8/30 22:34
 * @since 1.0
 * @discription 自定义注解处理器
 */
//指定此注解处理器处理的注解
@SupportedAnnotationTypes(value = "com.cloud.annotation.Getter")
//表示此注解处理器工作的jdk版本，这样是指定jdk1.8及以下
@SupportedSourceVersion(value = SourceVersion.RELEASE_8)
public class GetterAnnotaionProcess extends AbstractProcessor {
    /**
     *全局范围注解处理器编译过程日志记录
     */
    private Messager messager;
    /**
     *包含所有element的AST（包含所有元素的虚拟语法树）
     * 类、方法、属性等等都可以看成一个元素
     */
    private JavacTrees trees;
    /**
     *抽象树的模板（可以认为是一个抽象树节点的创建工厂，
     * 利用这个可以创建方法的抽象树、属性的抽象树、甚至是类的抽象树）
     */
    private TreeMaker treeMaker;
    /**
     *AST中所有的名称对象，包括方法的、属性的、类的等等
     */
    private Names names;

    /**
     *
     * 这里在编译加载此注解处理器时，
     * 将整个AST环境同步至自定义注解处理器，以便后续使用
     * @param processingEnv 全局AST环境
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    /**
     * 此方法是注解处理的重要方法，
     * 修改AST（抽象语法树）就是在此处
     * @param annotations 提供对指定类或接口的元素访问
     * @param roundEnv 全局元素环境（通过此参数可以获取到含有指定注解的元素）
     * @return Boolean 返回值表示是否后续注解处理器是否还需要处理（true表示不处理，false表示处理）
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //获取所有使用了Getter注解的元素
        Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(Getter.class);
        set.forEach(element -> {
            //获取元素的AST
            JCTree jcTree = trees.getTree(element);
            jcTree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    List<JCTree.JCVariableDecl> jcVariableDeclList = List.nil();
                    //遍历整个类AST
                    for (JCTree tree : jcClassDecl.defs) {
                        //如果是变量类型的树节点，将树节点放入集合中
                        if (tree.getKind().equals(Tree.Kind.VARIABLE)) {
                            JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
                            jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
                        }
                    }
                    //遍历所有的变量树节点
                    jcVariableDeclList.forEach(jcVariableDecl -> {
                        //加入注解处理器日志
                        messager.printMessage(Diagnostic.Kind.NOTE, jcVariableDecl.getName() + " has been processed");
                        //为每一个变量定义一个get方法，并且添加为新的类AST节点
                        jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                    });
                    super.visitClassDef(jcClassDecl);
                }

            });
        });
        return true;
    }

    /**
     * 为当前传入的JCTree 属性对象增加一个对应的get方法
     * @param jcVariableDecl 当前属性JCTree对象
     * @return
     */
    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        //拼接缓冲区
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        //拼接方法的body块，相当于写的code块
        statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        //使用treeMark定义方法，包括方法名称、方法私密修饰符、返回值类型
        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
    }

    /**
     * 根据属性名对象构建属性的get方法名称对象
     * @param name 属性名对象
     * @return
     */
    private Name getNewMethodName(Name name) {
        String s = name.toString();
        return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }
}
