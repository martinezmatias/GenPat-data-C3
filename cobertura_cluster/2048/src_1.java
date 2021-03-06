package net.sourceforge.cobertura.coveragedata;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import net.sourceforge.cobertura.instrument.pass3.AbstractCodeProvider;

public class TouchCollector implements HasBeenInstrumented {

	/*In fact - concurrentHashset*/
	private static Map<Class<?>,Integer> registeredClasses = new ConcurrentHashMap<Class<?>,Integer>();

	static {
		ProjectData.getGlobalProjectData(); // To call ProjectData.initialize();
	}
	
	public static synchronized void registerClass(Class<?> classa) {
		registeredClasses.put(classa,0);
	}

	public static synchronized void applyTouchesOnProjectData(
			ProjectData projectData) {
		System.out
				.println("=================== START OF RAPORT ======================== ");
		for (Class<?> c : registeredClasses.keySet()) {
			System.out.println("Report: "+c.getName());
			ClassData cd=projectData.getOrCreateClassData(c.getName());
			applyTouchesToSingleClassOnProjectData(cd, c);
		}
		System.out
				.println("===================  END OF RAPORT  ======================== ");

	}

	private static void applyTouchesToSingleClassOnProjectData(final ClassData classData,final Class<?> c) {
		System.out.println("----------- "+ c.getCanonicalName() + " ---------------- ");
		try {
			Method m0 = c.getDeclaredMethod(AbstractCodeProvider.COBERTURA_GET_AND_RESET_COUNTERS_METHOD_NAME);
			m0.setAccessible(true);
			final int[] res=(int[])m0.invoke(null, new Object[]{});
			
			LightClassmapListener lightClassmap=new ApplyToClassDataLightClassmapListener(classData,res);			
			Method m = c.getDeclaredMethod(AbstractCodeProvider.COBERTURA_CLASSMAP_METHOD_NAME,LightClassmapListener.class);
			m.setAccessible(true);
			m.invoke(null, lightClassmap);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	private static class ApplyToClassDataLightClassmapListener implements LightClassmapListener, HasBeenInstrumented  {
		//private AtomicInteger idProvider=new AtomicInteger(0);
		private final ClassData classData;
		private final int[] res;
		
		private int currentLine=0;
		private int jumpsInLine=0;
		private int switchesInLine=0;
		
		private void updateLine(int new_line){
			if(new_line!=currentLine){
				currentLine=new_line;
				jumpsInLine=0;
				switchesInLine=0;
			}
		}
		
		
		public ApplyToClassDataLightClassmapListener(ClassData cd,int[] res) {
			classData=cd;
			this.res=res;
		}
		
		public void setSource(String source) {
			System.out.println("source: "+source);
			classData.setSourceFileName(source);
			
		}
		
		public void setClazz(Class<?> clazz) {
//			System.out.println("class: "+clazz.getCanonicalName());					
		}
		
		public void putLineTouchPoint(int classLine, int counterId, String methodName, String methodDescription) {
			updateLine(classLine);
//			System.out.println("Line: "+methodName+":"+methodDescription+":"+classLine+":"+counterId +" = "+res[counterId]);				
			LineData ld=classData.addLine(classLine, methodName,methodDescription);
			ld.touch(res[counterId]);
		}
		
		public void putSwitchTouchPoint(int classLine,int... counterIds) {
			updateLine(classLine);
//			System.out.print("Switch in line: "+classLine+":"+Arrays.toString(counterIds)+" {");
//			for(int i=0; i<counterIds.length; i++){
//				System.out.print(res[counterIds[i]]+",");
//			}
//			System.out.println("}");
			
			LineData ld=getOrCreateLine(classLine);
			int switchId=switchesInLine++;
			classData.addLineSwitch(classLine,switchId , 0, counterIds.length-2);
			for(int i=0; i<counterIds.length; i++){
				ld.touchSwitch(switchId, i-1, res[counterIds[i]]);
			}
		}
		
		
		
		public void putJumpTouchPoint(int classLine, int trueCounterId,	int falseCounterId) {
			updateLine(classLine);
			//System.out.println("classData:"+classData.getBaseName()+" Jump in line: "+classLine+":"+trueCounterId+"[="+res[trueCounterId]+"], "+falseCounterId+"[="+res[falseCounterId]+"]");					
			LineData ld=getOrCreateLine(classLine);
			int branchId=jumpsInLine++;
			classData.addLineJump(classLine, branchId);
			ld.touchJump(branchId, true, res[trueCounterId]);
			ld.touchJump(branchId, false, res[falseCounterId]);
		}

		private LineData getOrCreateLine(int classLine) {
			LineData ld=classData.getLineData(classLine);
			if(ld==null){
				ld=classData.addLine(classLine, null, null);
			}
			return ld;
		}
	};


}
