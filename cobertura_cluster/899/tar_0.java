/*
 * Cobertura - http://cobertura.sourceforge.net/
 *
 * Cobertura is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Cobertura is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cobertura; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */

package net.sourceforge.cobertura.instrument.pass3;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.sourceforge.cobertura.coveragedata.HasBeenInstrumented;
import net.sourceforge.cobertura.instrument.AbstractFindTouchPointsClassInstrumenter;
import net.sourceforge.cobertura.instrument.FindTouchPointsMethodAdapter;
import net.sourceforge.cobertura.instrument.tp.ClassMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * <p>This class is responsible for real instrumentation of the user's class.</p> 
 * 
 * <p>It uses information acquired 
 * by {@link BuildClassMapClassVisitor} ( {@link #classMap} ) and 
 * {@link DetectDuplicatedCodeClassVisitor} and injects 
 * code snippet provided by {@link CodeProvider} ( {@link #codeProvider} ).</p>  
 * 
 * @author piotr.tabor@gmail.com
 */
public class InjectCodeClassInstrumenter extends AbstractFindTouchPointsClassInstrumenter{
	/**
	 * This class is responsible for injecting code inside 'interesting places' of methods inside instrumented class 
	 */
	private final InjectCodeTouchPointListener touchPointListener;

	/**
	 * {@link ClassMap} generated in previous instrumentation pass by {@link BuildClassMapClassVisitor}
	 */
	private final ClassMap classMap;
	
	/**
	 * {@link CodeProvider} used to generate pieces of asm code that is injected into instrumented class.
	 * 
	 * We are strictly recommending here using {@link FastArrayCodeProvider} instead of {@link AtomicArrayCodeProvider} because 
	 * of performance. 
	 */
	private final CodeProvider codeProvider;
	
	/**
	 * When we processing the class we want to now if we processed 'static initialization block' (clinit method). 
	 * 
	 *  <p>If there is no such a method in the instrumented class - we will need to generate it at the end</p>
	 */
	private boolean wasStaticInitMethodVisited=false;

	private final Set<String> ignoredMethods;
	
	/**
	 * @param cv                 - a listener for code-instrumentation events 
	 * @param ignoreRegexp       - list of patters of method calls that should be ignored from line-coverage-measurement
	 * @param classMap           - map of all interesting places in the class. You should acquire it by {@link BuildClassMapClassVisitor} and remember to 
	 * prepare it using {@link ClassMap#assignCounterIds()} before using it with {@link InjectCodeClassInstrumenter}  
	 * @param duplicatedLinesMap - map of found duplicates in the class. You should use {@link DetectDuplicatedCodeClassVisitor} to find the duplicated lines. 
	 */
	public InjectCodeClassInstrumenter(ClassVisitor cv, Collection<Pattern> ignoreRegexes, boolean threadsafeRigorous,
			ClassMap classMap,Map<Integer, Map<Integer, Integer>> duplicatedLinesMap,
			Set<String> ignoredMethods) {
		super(cv,ignoreRegexes,duplicatedLinesMap);		
		this.classMap=classMap;
		this.ignoredMethods = ignoredMethods;
		codeProvider = threadsafeRigorous ?  new AtomicArrayCodeProvider() : new FastArrayCodeProvider();
		touchPointListener=new InjectCodeTouchPointListener(classMap, codeProvider);		
	}
	
	/**
	 * <p>Marks the class 'already instrumented' and injects code connected to the fields that are keeping counters</p>
	 * 
	 *  <p>Marking the class is currently acquired by adding {@link HasBeenInstrumented} to the list of implemented interfaces</p>
	 *  <p>TODO: I would recommend instead of using interface, to use annotation. The benefit is that using annotation 
	 *  the instrumented code will not require cobertura.jar to work</p>    
	 */
	@Override
	public void visit(int version, int access, String name, String signature,
			String supertype, String[] interfaces) {	
		
		String[] new_interfaces = appendToTable(interfaces,Type.getInternalName(HasBeenInstrumented.class));	
		super.visit(version, access, name, signature, supertype, new_interfaces);		
		codeProvider.generateCountersField(cv);			}

	/**
	 * Creates a new table that is concatenation of given table (old_content) and a new_item
	 * 
	 * @param old_content - table that is coppied to returned table. The table is not modified.
	 * @param new_item - item to add at the end of the table
	 * @return
	 */
	private static  String[] appendToTable(String[] old_content,String new_item) {
		//JDK1.6: String[] new_interfaces=Arrays.copyOf(interfaces, interfaces.length+1);		
		String[] new_interfaces=new String[old_content.length+1];
		for(int i=0; i<old_content.length; i++){
			new_interfaces[i]=old_content[i];
		}
		new_interfaces[old_content.length]=new_item;
		return new_interfaces;
	}	

	/**
	 * <p>Instrumenting a code in a single method. Special conditions for processing 'static initialization block'.</p>
	 * 
	 * <p>This method also uses {@link ShiftVariableMethodAdapter} that is used firstly to calculate the index of internal
	 * variable injected to store information about last 'processed' jump or switch in runtime ( {@link ShiftVariableMethodAdapter#calculateFirstStackVariable(int, String)} ),
	 * and then is used to inject code responsible for keeping the variable and shifting (+1) all previously seen variables.    
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {			
		MethodVisitor mv = super.visitMethod(access, name, desc, signature,	exceptions);
		if (ignoredMethods.contains(name + desc)) {
			return mv;
		}
		if ("<clinit>".equals(name)){
			/*
			 * It is static initialization method, so we have to inject 'static initialization code'
			 * We will add this code after processing (instrumenting) previous content of static initialization code. 
			 * */
			mv=new GenerateCLINITMethodVisitor(mv,  classMap.getClassName(),classMap.getMaxCounterId()+1);
			wasStaticInitMethodVisited=true;
		}
		FindTouchPointsMethodAdapter instrumenter=new FindTouchPointsMethodAdapter(mv,classMap.getClassName(),name,desc,eventIdGenerator,duplicatedLinesMap,lineIdGenerator);
		instrumenter.setTouchPointListener(touchPointListener);
		instrumenter.setIgnoreRegexp(getIgnoreRegexp());
		touchPointListener.setLastJumpIdVariableIndex(ShiftVariableMethodAdapter.calculateFirstStackVariable(access, desc));
		return new ShiftVariableMethodAdapter(instrumenter, access, desc, 1);
	}
		
	/**
	 * Method instrumenter that injects {@link CodeProvider#generateCINITmethod(MethodVisitor, String, int)} code, and 
	 * then forwards the whole previous content of the method. 
	 * 
	 * @author piotr.tabor@gmail.com
	 */
	private class GenerateCLINITMethodVisitor extends MethodAdapter{
		private String className;
		private int counter_cnt;
		public GenerateCLINITMethodVisitor(MethodVisitor arg0,String className,int counter_cnt) {
			super(arg0);
			this.className=className;
			this.counter_cnt=counter_cnt;
		}

		@Override
		public void visitCode() {			
			super.visitCode();
			codeProvider.generateCINITmethod(mv, className,counter_cnt);
		}		
	}
	
	/**
	 * <p>If there was no 'static initialization block' in the class, the method is responsible for generating the method.<br/>
	 * It is also responsible for generating method that keeps mapping of counterIds into source places connected to them</p>
 
	 */
	@Override
	public void visitEnd() {
		if (!wasStaticInitMethodVisited){
			//We need to generate new method
			MethodVisitor mv=super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null,	null);
			mv.visitCode();
			codeProvider.generateCINITmethod(mv, classMap.getClassName(),classMap.getMaxCounterId()+1);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(/*stack*/3,/*local*/ 0);
			mv.visitEnd();			
			wasStaticInitMethodVisited=true;
		}
		
		codeProvider.generateCoberturaClassMapMethod(cv, classMap);
		codeProvider.generateCoberturaGetAndResetCountersMethod(cv, classMap.getClassName());
		
		super.visitEnd();
	}

	
}
