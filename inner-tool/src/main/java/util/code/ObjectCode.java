package util.code;

import common.Settings;
import util.code.block.ArrayCodeBlock;
import util.code.block.CodeBlock;
import util.code.block.CollectionCodeBlock;
import util.code.block.MapCodeBlock;
import util.code.block.PojoCodeBlock;

import java.util.Collection;
import java.util.Map;

public class ObjectCode {
    private final Settings settings;

    final int level;
    int constructorLevel;

    final String referenceName;
    public int referenceCount;
    boolean forceInline;

    private final String variableType;

    private final Object object;

    private CodeBlock<?> codeBlock;

    ObjectCode(Settings settings, int level, String referenceName, Object object, String variableType) {
        this.settings = settings;

        this.level = level;
        this.constructorLevel = level;

        this.referenceName = referenceName;
        this.variableType = variableType;

        this.object = object;
    }

    public void visitChildren(ObjectCodeGeneratorCore objectCodeGeneratorCore) {
        if (object.getClass().isArray()) {
            codeBlock = new ArrayCodeBlock(object, settings, level, referenceName);
        } else if (object instanceof Collection) {
            codeBlock = new CollectionCodeBlock((Collection<?>) object, settings, level, referenceName);
        } else if (object instanceof Map) {
            codeBlock = new MapCodeBlock((Map<?, ?>) object, settings, level, referenceName);
        } else {
            codeBlock = new PojoCodeBlock(object, settings, level, referenceName);
        }

        codeBlock.visitChildren(objectCodeGeneratorCore);
    }

    /**
     * generates both constructor and assignment
     */
    public String generateConstructorCodeWithAssignment() {
        return codeBlock.generateConstructorCodeWithAssignment(variableType);
    }

    /**
     * generates code for inlining
     */
    String generateInlineCode() {
        return codeBlock.generateInlineCode();
    }

    /**
     * generates constructor
     */
    String generateConstructorCode() {
        return codeBlock.generateConstructorCode(variableType);
    }

    /**
     * generates assignment
     */
    public String generateAssignmentCode() {
        return codeBlock.generateAssignmentCode();
    }

    public boolean hasEmptyAssignment() {
        return codeBlock.hasEmptyAssignment();
    }
}
