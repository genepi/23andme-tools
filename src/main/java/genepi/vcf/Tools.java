package genepi.vcf;

import java.lang.reflect.InvocationTargetException;

import genepi.base.Toolbox;

public class Tools extends Toolbox {

	public Tools(String command, String[] args) {
		
		super(command, args);
		
	}
	
	public static void main (String[] args){
		
		Tools tools = new Tools("jar vcf-tools.jar", args);
		
		tools.addTool("site-generator", GenerateSites.class);
		tools.addTool("vcf-generator", GenerateVCF.class);
		
		try {
			tools.start();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
