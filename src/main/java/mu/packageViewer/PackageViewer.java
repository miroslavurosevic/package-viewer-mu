package mu.packageViewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@SpringBootApplication
@Controller
public class PackageViewer {
	private TreeMap<String,PackageInfo> packages;

	public static void main(String[] args) {
		SpringApplication.run(PackageViewer.class, args);
	}

	@GetMapping("/")
	public String index(Model model) {
		if(packages==null) {
			parseStatusFile();
		}
		model.addAttribute("packages", packages);
		return "index";
	}
	
	@GetMapping("/info/{packageName}")
	public String packageInfo(@PathVariable String packageName, Model model) {
		model.addAttribute("packageName", packages.get(packageName).getName());
		model.addAttribute("packageDescription",packages.get(packageName).getDescription());
		model.addAttribute("dependencies",packages.get(packageName).getDependencies());
		model.addAttribute("alternativeDependencies",packages.get(packageName).getAlternativeDependencies());
		model.addAttribute("reverseDependencies", packages.get(packageName).getReverseDependencies());
		
		return "packageInfo";
	}
	
	
	private void parseStatusFile() {
		packages = new TreeMap<String, PackageInfo>();
		
		File statusFile = new File("/var/lib/dpkg/status");
		if(!statusFile.exists()) {
			statusFile = new File("status.real");
		} 
		
		String nextLine = null;
		String value = null;
		PackageInfo packageInfo = null;
		Boolean readNextLine = true;
		
		try {
			Scanner scanner = new Scanner(statusFile);
			while(scanner.hasNextLine()) {
				if(readNextLine) {
					nextLine = scanner.nextLine();
				} else {
					readNextLine = true;
				}
				
				if(nextLine.startsWith("Package:")) {
					value = nextLine.split(":")[1].trim();
					packageInfo = new PackageInfo(value);
				} else if(nextLine.startsWith("Description:")) {
					value = nextLine.split(":")[1].trim()+"<br>";
					nextLine = scanner.nextLine();
					while(nextLine.startsWith(" ")) {
						value = value.concat(nextLine);
						nextLine = scanner.nextLine();
					}
					packageInfo.addDescription(value);
					readNextLine = false;
				} else if(nextLine.startsWith("Depends:")) {
					value =  nextLine.split(":")[1].trim();
					value = value.replaceAll("\\([^,]*", "");
					value = value.replaceAll("\\|",",");
					String[] dependencies = value.split(",");
					packageInfo.addDependencies(dependencies);
				} else if(nextLine.isEmpty()) {
					packages.put(packageInfo.getName(),packageInfo);	
				}
			}
			scanner.close();
			
			//Add reverse dependencies and find not installed alternative dependencies
			//that should not be shown as links
			for(Map.Entry<String, PackageInfo> entry: packages.entrySet()) {
				for(String dependency:entry.getValue().getDependencies()) {
					if(packages.get(dependency.trim())!=null) {
						packages.get(dependency.trim()).addReverseDependency(entry.getKey());
					} else {
						entry.getValue().addAlternativeDependency(dependency);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	
	private class PackageInfo{
		private String name, description;
		private ArrayList<String> dependencies, alternativeDependencies, reverseDependencies;

		private PackageInfo(String name) {
			this.name = name;
			this.dependencies = new ArrayList<String>();
			this.alternativeDependencies = new ArrayList<String>();
			this.reverseDependencies = new ArrayList<String>();
		}
		
		private void addDescription(String description) {
			this.description = description;
			
		}
		
		private void addDependencies(String[] dependencies) {
			this.dependencies.addAll(Arrays.asList(dependencies));
			
		}
		
		private void addAlternativeDependency(String dependency) {
			this.alternativeDependencies.add(dependency);
		}
		
		private void addReverseDependency(String dependency) {
			this.reverseDependencies.add(dependency);
		}
		
		private String getName() {
			return this.name;
		}
		
		private String getDescription() {
			return this.description;
		}
		
		private ArrayList<String> getDependencies(){
			this.dependencies.sort(null);
			return this.dependencies;
		}
		
		private ArrayList<String> getAlternativeDependencies(){
			return this.alternativeDependencies;
		}
		
		private ArrayList<String> getReverseDependencies(){
			this.reverseDependencies.sort(null);
			return this.reverseDependencies;
		}
	}

}
