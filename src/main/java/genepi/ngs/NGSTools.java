package genepi.ngs;

import java.lang.reflect.InvocationTargetException;

import genepi.base.Toolbox;

public class NGSTools extends Toolbox {

	public NGSTools(String command, String[] args) {
		
		super(command, args);
		
	}
	
	public static void main (String[] args){
		
		NGSTools tools = new NGSTools("java -jar ngs-tools.jar", args);
		
		tools.addTool("site-generator", GenerateSites.class);
		
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
