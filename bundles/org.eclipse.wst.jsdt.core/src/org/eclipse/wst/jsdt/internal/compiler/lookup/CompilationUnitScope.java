/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Erling Ellingsen -  patch for bug 125570
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.wst.jsdt.core.ClasspathContainerInitializer;
import org.eclipse.wst.jsdt.core.IClasspathEntry;
import org.eclipse.wst.jsdt.core.IPackageFragment;
import org.eclipse.wst.jsdt.core.JavaCore;
import org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.compiler.libraries.SystemLibraryLocation;
import org.eclipse.wst.jsdt.core.dom.AST;
import org.eclipse.wst.jsdt.core.dom.Type;
import org.eclipse.wst.jsdt.internal.compiler.ast.*;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.impl.Constant;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.*;
import org.eclipse.wst.jsdt.internal.core.JavaProject;
import org.eclipse.wst.jsdt.internal.core.PackageFragment;
import org.eclipse.wst.jsdt.internal.infer.InferredAttribute;
import org.eclipse.wst.jsdt.internal.infer.InferredMethod;
import org.eclipse.wst.jsdt.internal.infer.InferredType;

import sun.misc.FpUtils;

public class CompilationUnitScope extends BlockScope {
	
public LookupEnvironment environment;
public CompilationUnitDeclaration referenceContext;
public char[][] currentPackageName;
//public PackageBinding fPackage;
public ImportBinding[] imports;
public HashtableOfObject typeOrPackageCache; // used in Scope.getTypeOrPackage()

public SourceTypeBinding[] topLevelTypes;

private CompoundNameVector qualifiedReferences;
private SimpleNameVector simpleNameReferences;
private ObjectVector referencedTypes;
private ObjectVector referencedSuperTypes;

HashtableOfType constantPoolNameUsage;
public int analysisIndex;
private int captureID = 1;

/* Allows a compilation unit to inherit fields from a superType */
public ReferenceBinding superBinding;
private MethodScope methodScope;
private ClassScope classScope;





public CompilationUnitScope(CompilationUnitDeclaration unit, LookupEnvironment environment) {
	super(COMPILATION_UNIT_SCOPE, null);

	this.environment = environment;
	this.referenceContext = unit;
	unit.scope = this;
	
	char [][]pkgName= unit.currentPackage == null ? unit.compilationResult.getPackageName() : unit.currentPackage.tokens;
	
	this.currentPackageName = pkgName == null ? CharOperation.NO_CHAR_CHAR : pkgName;
 
	
	this.referencedTypes = new ObjectVector();
	if (compilerOptions().produceReferenceInfo) {
		this.qualifiedReferences = new CompoundNameVector();
		this.simpleNameReferences = new SimpleNameVector();
		this.referencedSuperTypes = new ObjectVector();
	} else {
		this.qualifiedReferences = null; // used to test if dependencies should be recorded
		this.simpleNameReferences = null;
//		this.referencedTypes = null;
		this.referencedSuperTypes = null;
	}
	
}

public MethodScope methodScope() {
	if(superBinding!=null && methodScope==null) {
		methodScope = new MethodScope(classScope,referenceContext(),false);
	}
	
	return methodScope;
}

public ClassScope classScope() {
	if (this.classScope!=null) return this.classScope;
	return super.classScope();
}

void buildFieldsAndMethods() {
	for (int i = 0, length = topLevelTypes.length; i < length; i++)
		topLevelTypes[i].buildFieldsAndMethods();
}
void buildTypeBindings(AccessRestriction accessRestriction) {
	topLevelTypes = new SourceTypeBinding[0]; // want it initialized if the package cannot be resolved
	boolean firstIsSynthetic = false;
//	if (referenceContext.compilationResult.compilationUnit != null) {
//		char[][] expectedPackageName = referenceContext.compilationResult.compilationUnit.getPackageName();
//		if (expectedPackageName != null 
//				&& !CharOperation.equals(currentPackageName, expectedPackageName)) {
//
//			// only report if the unit isn't structurally empty
//			if (referenceContext.currentPackage != null 
//					|| referenceContext.types != null 
//					|| referenceContext.imports != null) {
//				problemReporter().packageIsNotExpectedPackage(referenceContext);
//			}
//			currentPackageName = expectedPackageName.length == 0 ? CharOperation.NO_CHAR_CHAR : expectedPackageName;
//		}
//	}
//	if (currentPackageName == CharOperation.NO_CHAR_CHAR) {
//		if ((fPackage = environment.defaultPackage) == null) {
//			problemReporter().mustSpecifyPackage(referenceContext);
//			return;
//		}
//	} else {
//		if ((fPackage = environment.createPackage(currentPackageName)) == null) {
//			problemReporter().packageCollidesWithType(referenceContext);
//			return;
//		} else if (referenceContext.isPackageInfo()) {
//			// resolve package annotations now if this is "package-info.js".				
//			if (referenceContext.types == null || referenceContext.types.length == 0) {
//				referenceContext.types = new TypeDeclaration[1];
//				TypeDeclaration declaration = new TypeDeclaration(referenceContext.compilationResult);
//				referenceContext.types[0] = declaration;
//				declaration.name = TypeConstants.PACKAGE_INFO_NAME;
//				declaration.modifiers = ClassFileConstants.AccDefault | ClassFileConstants.AccInterface;
//				firstIsSynthetic = true;
//			}
//		}
//		recordQualifiedReference(currentPackageName); // always dependent on your own package
//	}
	
//	// Skip typeDeclarations which know of previously reported errors
//	TypeDeclaration[] types = referenceContext.types;
//	int typeLength = (types == null) ? 0 : types.length;
//	topLevelTypes = new SourceTypeBinding[typeLength];
//	int count = 0;
//	nextType: for (int i = 0; i < typeLength; i++) {
//		TypeDeclaration typeDecl = types[i];
//		ReferenceBinding typeBinding = fPackage.getType0(typeDecl.name);
//		recordSimpleReference(typeDecl.name); // needed to detect collision cases
//		if (typeBinding != null && !(typeBinding instanceof UnresolvedReferenceBinding)) {
//			// if a type exists, it must be a valid type - cannot be a NotFound problem type
//			// unless its an unresolved type which is now being defined
//			problemReporter().duplicateTypes(referenceContext, typeDecl);
//			continue nextType;
//		}
//		if (fPackage != environment.defaultPackage && fPackage.getPackage(typeDecl.name) != null) {
//			// if a package exists, it must be a valid package - cannot be a NotFound problem package
//			problemReporter().typeCollidesWithPackage(referenceContext, typeDecl);
//			continue nextType;
//		}
//
//		if ((typeDecl.modifiers & ClassFileConstants.AccPublic) != 0) {
//			char[] mainTypeName;
//			if ((mainTypeName = referenceContext.getMainTypeName()) != null // mainTypeName == null means that implementor of ICompilationUnit decided to return null
//					&& !CharOperation.equals(mainTypeName, typeDecl.name)) {
//				problemReporter().publicClassMustMatchFileName(referenceContext, typeDecl);
//				// tolerate faulty main type name (91091), allow to proceed into type construction
//			}
//		}
//
//		ClassScope child = new ClassScope(this, typeDecl);
//		SourceTypeBinding type = child.buildType(null, fPackage, accessRestriction);
//		if (firstIsSynthetic && i == 0)
//			type.modifiers |= ClassFileConstants.AccSynthetic;
//		if (type != null)
//			topLevelTypes[count++] = type;
//	}
//
//	// shrink topLevelTypes... only happens if an error was reported
//	if (count != topLevelTypes.length)
//		System.arraycopy(topLevelTypes, 0, topLevelTypes = new SourceTypeBinding[count], 0, count);
//	

	
	// Skip typeDeclarations which know of previously reported errors
	int typeLength = referenceContext.numberInferredTypes;
	
	/* Include super type whild building */
//	if(superTypeName!=null) {
//		superType = environment.askForType(new char[][] {superTypeName});
//	}

//			//((SourceTypeBinding)superType).classScope.buildInferredType(null, environment.defaultPackage,accessRestriction);
//			//((SourceTypeBinding)superType).classScope.connectTypeHierarchy();
//			//FieldBinding[] fields = superType.fields();
//			//addSubscope(((SourceTypeBinding)superType).classScope);
//			
//			
//		//	this.parent = ((SourceTypeBinding)superType).classScope;
//			
//			
//		}
//	
//		
//	}
	
	/* may need to get the actual binding here */
//	if(libSuperType!=null) {
//		//ClasspathContainerInitializer cinit = libSuperType.getContainerInitializer();
//		//IClasspathEntry[] entries = libSuperType.getClasspathEntries();
//		IPackageFragment[] fragments = libSuperType.getPackageFragments();
//		for(int i = 0;i<fragments.length;i++) {
//			String packageName = fragments[i].getElementName();
//			PackageBinding binding = environment.getPackage0(packageName.toCharArray());
//			superBinding  = binding.getType(libSuperType.getSuperTypeName().toCharArray());
//			if(superBinding!=null) break;
//			
//		}
//		
//	}else 
		
	
	 buildSuperType();
	
	topLevelTypes = new SourceTypeBinding[typeLength];
	
	int count = 0;
	
	
	nextType: for (int i = 0; i < typeLength; i++) {
		InferredType typeDecl =  referenceContext.inferredTypes[i];
		if (typeDecl.isDefinition) {
			ReferenceBinding typeBinding = environment.defaultPackage
					.getType0(typeDecl.getName());
			recordSimpleReference(typeDecl.getName()); // needed to detect collision cases
			if (typeBinding != null
					&& !(typeBinding instanceof UnresolvedReferenceBinding)) {
				// if a type exists, it must be a valid type - cannot be a NotFound problem type
				// unless its an unresolved type which is now being defined
				problemReporter().duplicateTypes(referenceContext, typeDecl);
				continue nextType;
			}
			ClassScope child = new ClassScope(this, typeDecl);
			SourceTypeBinding type = child.buildInferredType(null, environment.defaultPackage,
					accessRestriction);
			//		SourceTypeBinding type = buildType(typeDecl,null, fPackage, accessRestriction);
			if (type != null)
				topLevelTypes[count++] = type;
		}		
	}

	// shrink topLevelTypes... only happens if an error was reported
	if (count != topLevelTypes.length)
		System.arraycopy(topLevelTypes, 0, topLevelTypes = new SourceTypeBinding[count], 0, count);
	

	
	char [] path=CharOperation.concatWith(this.currentPackageName, '/');
	referenceContext.compilationUnitBinding=new CompilationUnitBinding(this,environment.defaultPackage,path, superBinding);
	
	ArrayList methods=new ArrayList();
	ArrayList vars=new ArrayList();
	ArrayList stmts=new ArrayList();
	for (int i = 0; i < this.referenceContext.statements.length; i++) {
		ProgramElement element = this.referenceContext.statements[i];
		if (element instanceof MethodDeclaration /* && ((MethodDeclaration)element).selector!=null */) {
			methods.add(element);
		}
		else if (element instanceof LocalDeclaration) {
			vars.add(element);
		}
		else
			stmts.add(element);
	}
	buildMethods(methods);
	buildVars(vars);
}

public void buildSuperType() {
	
	char[] superTypeName = null;
	LibrarySuperType libSuperType = null;
	if(this.referenceContext.compilationResult!=null && this.referenceContext.compilationResult.compilationUnit!=null) {
		libSuperType = this.referenceContext.compilationResult.compilationUnit.getCommonSuperType();
		if(libSuperType==null) {
			superTypeName = null;
			return;
		}else
			superTypeName = libSuperType.getSuperTypeName().toCharArray();
	}
	if (superTypeName==null)
		return;
	
		superBinding  =  environment.askForType(new char[][] {superTypeName});
	
		if(superBinding==null || !superBinding.isValidBinding()) {
			superTypeName = null;
			return ;
		}
		
	
		/* build methods */
		if(superBinding!=null) {
			InferredType te = superBinding.getInferredType();
			classScope = new ClassScope(this, te);
			
			SourceTypeBinding sourceType = null;
			
			if(superBinding instanceof SourceTypeBinding) {
				sourceType = (SourceTypeBinding)superBinding;
			}
			classScope.buildInferredType(sourceType, environment.defaultPackage, null);
			
			
			recordTypeReference(superBinding);
			recordSuperTypeReference(superBinding);
			environment().setAccessRestriction(superBinding, null);	
		}
		
		
		
		
		
	
	
	if(superTypeName!=null && superTypeName.length==0) {
		superTypeName=null;
	}
}
public TypeVariableBinding[] createTypeVariables(TypeParameter[] typeParameters, Binding declaringElement) {
	// do not construct type variables if source < 1.5
	if (typeParameters == null || compilerOptions().sourceLevel < ClassFileConstants.JDK1_5)
		return Binding.NO_TYPE_VARIABLES;

	PackageBinding unitPackage = compilationUnitScope().getDefaultPackage();
	int length = typeParameters.length;
	TypeVariableBinding[] typeVariableBindings = new TypeVariableBinding[length];
	int count = 0;
	for (int i = 0; i < length; i++) {
		TypeParameter typeParameter = typeParameters[i];
		TypeVariableBinding parameterBinding = new TypeVariableBinding(typeParameter.name, declaringElement, i);
		parameterBinding.fPackage = unitPackage;
		typeParameter.binding = parameterBinding;

		// detect duplicates, but keep each variable to reduce secondary errors with instantiating this generic type (assume number of variables is correct)
		for (int j = 0; j < count; j++) {
			TypeVariableBinding knownVar = typeVariableBindings[j];
			if (CharOperation.equals(knownVar.sourceName, typeParameter.name))
				problemReporter().duplicateTypeParameterInType(typeParameter);
		}
		typeVariableBindings[count++] = parameterBinding;
//			TODO should offer warnings to inform about hiding declaring, enclosing or member types				
//			ReferenceBinding type = sourceType;
//			// check that the member does not conflict with an enclosing type
//			do {
//				if (CharOperation.equals(type.sourceName, memberContext.name)) {
//					problemReporter().hidingEnclosingType(memberContext);
//					continue nextParameter;
//				}
//				type = type.enclosingType();
//			} while (type != null);
//			// check that the member type does not conflict with another sibling member type
//			for (int j = 0; j < i; j++) {
//				if (CharOperation.equals(referenceContext.memberTypes[j].name, memberContext.name)) {
//					problemReporter().duplicateNestedType(memberContext);
//					continue nextParameter;
//				}
//			}
	}
	if (count != length)
		System.arraycopy(typeVariableBindings, 0, typeVariableBindings = new TypeVariableBinding[count], 0, count);
	return typeVariableBindings;
}
SourceTypeBinding buildType(InferredType inferredType, SourceTypeBinding enclosingType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
	// provide the typeDeclaration with needed scopes

	if (enclosingType == null) {
		char[][] className = CharOperation.arrayConcat(packageBinding.compoundName, inferredType.getName());
		inferredType.binding = new SourceTypeBinding(className, packageBinding, this);
	} else {
//		char[][] className = CharOperation.deepCopy(enclosingType.compoundName);
//		className[className.length - 1] =
//			CharOperation.concat(className[className.length - 1], inferredType.getName(), '$');
//		inferredType.binding = new MemberTypeBinding(className, this, enclosingType);
	}

	SourceTypeBinding sourceType = inferredType.binding;
	environment().setAccessRestriction(sourceType, accessRestriction);		
	sourceType.fPackage.addType(sourceType);
	return sourceType;
}

private void buildVars(ArrayList vars) {
	for (Iterator iter = vars.iterator(); iter.hasNext();) {
		LocalDeclaration localVar = (LocalDeclaration) iter.next();
//		localVar.resolve(this);
		LocalVariableBinding binding = new LocalVariableBinding(localVar, null, 0, false);
		localVar.binding=binding;
		addLocalVariable(binding);
		binding.setConstant(Constant.NotAConstant);
	}
}

private void buildMethods(ArrayList methods) {
	if (methods.size()==0) {
		referenceContext.compilationUnitBinding.setMethods(Binding.NO_METHODS);
		return;
	} 


	MethodBinding[] methodBindings = new MethodBinding[methods.size()];
	// create bindings for source methods
	int count=0;
	for (Iterator iter = methods.iterator(); iter.hasNext();) {
		AbstractMethodDeclaration method = (AbstractMethodDeclaration) iter.next();
			MethodScope scope = new MethodScope(this,method, false);
			MethodBinding methodBinding = scope.createMethod(method,method.selector,(SourceTypeBinding)referenceContext.compilationUnitBinding,false,false);
			if (methodBinding != null && methodBinding.selector!=null) // is null if binding could not be created
				methodBindings[count++] = methodBinding;
			if (methodBinding.selector!=null)
				environment.defaultPackage.addBinding(methodBinding, methodBinding.selector,Binding.METHOD);
			method.binding=methodBinding;
	}
	if (count != methodBindings.length)
		System.arraycopy(methodBindings, 0, methodBindings = new MethodBinding[count], 0, count);
	referenceContext.compilationUnitBinding.setMethods(methodBindings);
}


public PackageBinding getDefaultPackage() {
		return environment.defaultPackage;
}

public  void addLocalVariable(LocalVariableBinding binding) {
	super.addLocalVariable(binding);
	environment.defaultPackage.addBinding(binding, binding.name, Binding.VARIABLE);
}

void checkAndSetImports() {
	if (referenceContext.imports == null) {
		imports = getDefaultImports();
		return;
	}

	// allocate the import array, add java.lang.* by default
	int numberOfStatements = referenceContext.imports.length;
	int numberOfImports = numberOfStatements + 1;
	for (int i = 0; i < numberOfStatements; i++) {
		ImportReference importReference = referenceContext.imports[i];
		if (((importReference.bits & ASTNode.OnDemand) != 0) && CharOperation.equals(JAVA_LANG, importReference.tokens) && !importReference.isStatic()) {
			numberOfImports--;
			break;
		}
	}
	ImportBinding[] resolvedImports = new ImportBinding[numberOfImports];
	resolvedImports[0] = getDefaultImports()[0];
	int index = 1;

	nextImport : for (int i = 0; i < numberOfStatements; i++) {
		ImportReference importReference = referenceContext.imports[i];
		char[][] compoundName = importReference.tokens;

		// skip duplicates or imports of the current package
		for (int j = 0; j < index; j++) {
			ImportBinding resolved = resolvedImports[j];
			if (resolved.onDemand == ((importReference.bits & ASTNode.OnDemand) != 0) && resolved.isStatic() == importReference.isStatic())
				if (CharOperation.equals(compoundName, resolvedImports[j].compoundName))
					continue nextImport;
		}

		if ((importReference.bits & ASTNode.OnDemand) != 0) {
			if (CharOperation.equals(compoundName, currentPackageName))
				continue nextImport;

			Binding importBinding = findImport(compoundName, compoundName.length);
			if (!importBinding.isValidBinding() || (importReference.isStatic() && importBinding instanceof PackageBinding))
				continue nextImport;	// we report all problems in faultInImports()
			resolvedImports[index++] = new ImportBinding(compoundName, true, importBinding, importReference);
		} else {
			// resolve single imports only when the last name matches
			resolvedImports[index++] = new ImportBinding(compoundName, false, null, importReference);
		}
	}

	// shrink resolvedImports... only happens if an error was reported
	if (resolvedImports.length > index)
		System.arraycopy(resolvedImports, 0, resolvedImports = new ImportBinding[index], 0, index);
	imports = resolvedImports;
}

/**
 * Perform deferred check specific to parameterized types: bound checks, supertype collisions
 */
void checkParameterizedTypes() {
	if (compilerOptions().sourceLevel < ClassFileConstants.JDK1_5) return;

	for (int i = 0, length = topLevelTypes.length; i < length; i++) {
		ClassScope scope = topLevelTypes[i].classScope;
		scope.checkParameterizedTypeBounds();
		scope.checkParameterizedSuperTypeCollisions();
	}
}
/*
 * INTERNAL USE-ONLY
 * Innerclasses get their name computed as they are generated, since some may not
 * be actually outputed if sitting inside unreachable code.
 */
public char[] computeConstantPoolName(LocalTypeBinding localType) {
	if (localType.constantPoolName() != null) {
		return localType.constantPoolName();
	}
	// delegates to the outermost enclosing classfile, since it is the only one with a global vision of its innertypes.

	if (constantPoolNameUsage == null)
		constantPoolNameUsage = new HashtableOfType();

	ReferenceBinding outerMostEnclosingType = localType.scope.outerMostClassScope().enclosingSourceType();
	
	// ensure there is not already such a local type name defined by the user
	int index = 0;
	char[] candidateName;
	boolean isCompliant15 = compilerOptions().complianceLevel >= ClassFileConstants.JDK1_5;
	while(true) {
		if (localType.isMemberType()){
			if (index == 0){
				candidateName = CharOperation.concat(
					localType.enclosingType().constantPoolName(),
					localType.sourceName,
					'$');
			} else {
				// in case of collision, then member name gets extra $1 inserted
				// e.g. class X { { class L{} new X(){ class L{} } } }
				candidateName = CharOperation.concat(
					localType.enclosingType().constantPoolName(),
					'$',
					String.valueOf(index).toCharArray(),
					'$',
					localType.sourceName);
			}
		} else if (localType.isAnonymousType()){
			if (isCompliant15) {
				// from 1.5 on, use immediately enclosing type name
				candidateName = CharOperation.concat(
					localType.enclosingType.constantPoolName(),
					String.valueOf(index+1).toCharArray(),
					'$');
			} else {
				candidateName = CharOperation.concat(
					outerMostEnclosingType.constantPoolName(),
					String.valueOf(index+1).toCharArray(),
					'$');
			}
		} else {
			// local type
			if (isCompliant15) {
				candidateName = CharOperation.concat(
					CharOperation.concat(
						localType.enclosingType().constantPoolName(),
						String.valueOf(index+1).toCharArray(),
						'$'),
					localType.sourceName);
			} else {
				candidateName = CharOperation.concat(
					outerMostEnclosingType.constantPoolName(),
					'$',
					String.valueOf(index+1).toCharArray(),
					'$',
					localType.sourceName);
			}
		}						
		if (constantPoolNameUsage.get(candidateName) != null) {
			index ++;
		} else {
			constantPoolNameUsage.put(candidateName, localType);
			break;
		}
	}
	return candidateName;
}

void connectTypeHierarchy() {

	
	//	if(superType!=null) {
//			if(superType instanceof SourceTypeBinding) {
//				((SourceTypeBinding)superType).classScope.buildFieldsAndMethods();
//				((SourceTypeBinding)superType).classScope.connectTypeHierarchy();
//				
//			}
//			ReferenceBinding[] memberTypes = superType.memberTypes();
//			ReferenceBinding[] memberFields = superType.typeVariables();
//			MethodBinding[] memberMethods = superType.availableMethods();
//			for(int i=0;i<memberTypes.length;i++) {
//				recordReference(memberTypes[i], memberTypes[i].sourceName);
//			}
//		}
	
//	if(superTypeName!=null) {
//		ReferenceBinding binding = environment.askForType(new char[][] {superTypeName});
//		this.recordSuperTypeReference(binding);
//	}
	if(classScope!=null) classScope.connectTypeHierarchy();
		for (int i=0;i<referenceContext.numberInferredTypes;i++) {
			InferredType inferredType = referenceContext.inferredTypes[i];
			if (inferredType.binding!=null)
 			  inferredType.binding.classScope.connectTypeHierarchy();
			
		}
}
void faultInImports() {
	if (this.typeOrPackageCache != null)
		return; // can be called when a field constant is resolved before static imports
	if (referenceContext.imports == null) {
		this.typeOrPackageCache = new HashtableOfObject(1);
		return;
	}

	// collect the top level type names if a single type import exists
	int numberOfStatements = referenceContext.imports.length;
	HashtableOfType typesBySimpleNames = null;
	for (int i = 0; i < numberOfStatements; i++) {
		if ((referenceContext.imports[i].bits & ASTNode.OnDemand) == 0) {
			typesBySimpleNames = new HashtableOfType(topLevelTypes.length + numberOfStatements);
			for (int j = 0, length = topLevelTypes.length; j < length; j++)
				typesBySimpleNames.put(topLevelTypes[j].sourceName, topLevelTypes[j]);
			break;
		}
	}

	// allocate the import array, add java.lang.* by default
	int numberOfImports = numberOfStatements + 1;
	for (int i = 0; i < numberOfStatements; i++) {
		ImportReference importReference = referenceContext.imports[i];
		if (((importReference.bits & ASTNode.OnDemand) != 0) && CharOperation.equals(JAVA_LANG, importReference.tokens) && !importReference.isStatic()) {
			numberOfImports--;
			break;
		}
	}
	ImportBinding[] resolvedImports = new ImportBinding[numberOfImports];
	resolvedImports[0] = getDefaultImports()[0];
	int index = 1;

	// keep static imports with normal imports until there is a reason to split them up
	// on demand imports continue to be packages & types. need to check on demand type imports for fields/methods
	// single imports change from being just types to types or fields
	nextImport : for (int i = 0; i < numberOfStatements; i++) {
		ImportReference importReference = referenceContext.imports[i];
		char[][] compoundName = importReference.tokens;

		// skip duplicates or imports of the current package
		for (int j = 0; j < index; j++) {
			ImportBinding resolved = resolvedImports[j];
			if (resolved.onDemand == ((importReference.bits & ASTNode.OnDemand) != 0) && resolved.isStatic() == importReference.isStatic()) {
				if (CharOperation.equals(compoundName, resolved.compoundName)) {
					problemReporter().unusedImport(importReference); // since skipped, must be reported now
					continue nextImport;
				}
			}
		}
		if ((importReference.bits & ASTNode.OnDemand) != 0) {
			if (CharOperation.equals(compoundName, currentPackageName)) {
				problemReporter().unusedImport(importReference); // since skipped, must be reported now
				continue nextImport;
			}

			Binding importBinding = findImport(compoundName, compoundName.length);
			if (!importBinding.isValidBinding()) {
				problemReporter().importProblem(importReference, importBinding);
				continue nextImport;
			}
			if (importReference.isStatic() && importBinding instanceof PackageBinding) {
				problemReporter().cannotImportPackage(importReference);
				continue nextImport;
			}
			resolvedImports[index++] = new ImportBinding(compoundName, true, importBinding, importReference);
		} else {
			Binding importBinding = findSingleImport(compoundName, importReference.isStatic());
			if (!importBinding.isValidBinding()) {
				problemReporter().importProblem(importReference, importBinding);
				continue nextImport;
			}
			if (importBinding instanceof PackageBinding) {
				problemReporter().cannotImportPackage(importReference);
				continue nextImport;
			}
			ReferenceBinding conflictingType = null;
			if (importBinding instanceof MethodBinding) {
				conflictingType = (ReferenceBinding) getType(compoundName, compoundName.length);
				if (!conflictingType.isValidBinding())
					conflictingType = null;
			}
			// collisions between an imported static field & a type should be checked according to spec... but currently not by javac
			if (importBinding instanceof ReferenceBinding || conflictingType != null) {
				ReferenceBinding referenceBinding = conflictingType == null ? (ReferenceBinding) importBinding : conflictingType;
				if (importReference.isTypeUseDeprecated(referenceBinding, this))
					problemReporter().deprecatedType(referenceBinding, importReference);

				ReferenceBinding existingType = typesBySimpleNames.get(compoundName[compoundName.length - 1]);
				if (existingType != null) {
					// duplicate test above should have caught this case, but make sure
					if (existingType == referenceBinding)
						continue nextImport;
					// either the type collides with a top level type or another imported type
					for (int j = 0, length = topLevelTypes.length; j < length; j++) {
						if (CharOperation.equals(topLevelTypes[j].sourceName, existingType.sourceName)) {
							problemReporter().conflictingImport(importReference);
							continue nextImport;
						}
					}
					problemReporter().duplicateImport(importReference);
					continue nextImport;
				}
				typesBySimpleNames.put(compoundName[compoundName.length - 1], referenceBinding);
			} else if (importBinding instanceof FieldBinding) {
				for (int j = 0; j < index; j++) {
					ImportBinding resolved = resolvedImports[j];
					// find other static fields with the same name
					if (resolved.isStatic() && resolved.resolvedImport instanceof FieldBinding && importBinding != resolved.resolvedImport) {
						if (CharOperation.equals(compoundName[compoundName.length - 1], resolved.compoundName[resolved.compoundName.length - 1])) {
							problemReporter().duplicateImport(importReference);
							continue nextImport;
						}
					}
				}
			}
			resolvedImports[index++] = conflictingType == null
				? new ImportBinding(compoundName, false, importBinding, importReference)
				: new ImportConflictBinding(compoundName, importBinding, conflictingType, importReference);
		}
	}

	// shrink resolvedImports... only happens if an error was reported
	if (resolvedImports.length > index)
		System.arraycopy(resolvedImports, 0, resolvedImports = new ImportBinding[index], 0, index);
	imports = resolvedImports;

	int length = imports.length;
	this.typeOrPackageCache = new HashtableOfObject(length);
	for (int i = 0; i < length; i++) {
		ImportBinding binding = imports[i];
		if (!binding.onDemand && binding.resolvedImport instanceof ReferenceBinding || binding instanceof ImportConflictBinding)
			this.typeOrPackageCache.put(binding.compoundName[binding.compoundName.length - 1], binding);
	}
}
public void faultInTypes() {
	faultInImports();

	this.referenceContext.compilationUnitBinding.faultInTypesForFieldsAndMethods();
	for (int i = 0, length = topLevelTypes.length; i < length; i++)
		topLevelTypes[i].faultInTypesForFieldsAndMethods();
}

//this API is for code assist purpose
public Binding findImport(char[][] compoundName, boolean findStaticImports, boolean onDemand) {
	if(onDemand) {
		return findImport(compoundName, compoundName.length);
	} else {
		return findSingleImport(compoundName, findStaticImports);
	}
}

private Binding findImport(char[][] compoundName, int length) {
	recordQualifiedReference(compoundName);

	Binding binding = environment.getTopLevelPackage(compoundName[0]);
	int i = 1;
	foundNothingOrType: if (binding != null) {
		PackageBinding packageBinding = (PackageBinding) binding;
		while (i < length) {
			binding = packageBinding.getTypeOrPackage(compoundName[i++], Binding.TYPE | Binding.PACKAGE);
			if (binding == null || !binding.isValidBinding()) {
				binding = null;
				break foundNothingOrType;
			}
			if (!(binding instanceof PackageBinding))
		 		break foundNothingOrType;

			packageBinding = (PackageBinding) binding;
		}
		return packageBinding;
	}

	ReferenceBinding type;
	if (binding == null) {
		if (environment.defaultPackage == null || compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
			return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), null, ProblemReasons.NotFound);
		type = findType(compoundName[0], environment.defaultPackage, environment.defaultPackage);
		if (type == null || !type.isValidBinding())
			return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), null, ProblemReasons.NotFound);
		i = 1; // reset to look for member types inside the default package type
	} else {
		type = (ReferenceBinding) binding;
	}

	while (i < length) {
		type = (ReferenceBinding)environment.convertToRawType(type); // type imports are necessarily raw for all except last
		if (!type.canBeSeenBy(environment.defaultPackage))
			return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), type, ProblemReasons.NotVisible);

		char[] name = compoundName[i++];
		// does not look for inherited member types on purpose, only immediate members
		type = type.getMemberType(name);
		if (type == null)
			return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, i), null, ProblemReasons.NotFound);
	}
	if (!type.canBeSeenBy(environment.defaultPackage))
		return new ProblemReferenceBinding(compoundName, type, ProblemReasons.NotVisible);
	return type;
}
private Binding findSingleImport(char[][] compoundName, boolean findStaticImports) {
	if (compoundName.length == 1) {
		// findType records the reference
		// the name cannot be a package
		if (environment.defaultPackage == null || compilerOptions().complianceLevel >= ClassFileConstants.JDK1_4)
			return new ProblemReferenceBinding(compoundName, null, ProblemReasons.NotFound);
		ReferenceBinding typeBinding = findType(compoundName[0], environment.defaultPackage, environment.defaultPackage);
		if (typeBinding == null)
			return new ProblemReferenceBinding(compoundName, null, ProblemReasons.NotFound);
		return typeBinding;
	}

	if (findStaticImports)
		return findSingleStaticImport(compoundName);
	return findImport(compoundName, compoundName.length);
}
private Binding findSingleStaticImport(char[][] compoundName) {
	Binding binding = findImport(compoundName, compoundName.length - 1);
	if (!binding.isValidBinding()) return binding;

	char[] name = compoundName[compoundName.length - 1];
	if (binding instanceof PackageBinding) {
		Binding temp = ((PackageBinding) binding).getTypeOrPackage(name,  Binding.TYPE | Binding.PACKAGE);
		if (temp != null && temp instanceof ReferenceBinding) // must resolve to a member type or field, not a top level type
			return new ProblemReferenceBinding(compoundName, (ReferenceBinding) temp, ProblemReasons.InvalidTypeForStaticImport);
		return binding; // cannot be a package, error is caught in sender
	}

	// look to see if its a static field first
	ReferenceBinding type = (ReferenceBinding) binding;
	FieldBinding field = findField(type, name, null, true);
	if (field != null && field.isValidBinding() && field.isStatic() && field.canBeSeenBy(type, null, this))
		return field;

	// look to see if there is a static method with the same selector
	MethodBinding method = findStaticMethod(type, name);
	if (method != null) return method;

	type = findMemberType(name, type);
	if (type == null || !type.isStatic()) {
		if (field != null && !field.isValidBinding() && field.problemId() != ProblemReasons.NotFound)
			return field;
		return new ProblemReferenceBinding(compoundName, type, ProblemReasons.NotFound);
	}
	if (!type.canBeSeenBy(environment.defaultPackage))
		return new ProblemReferenceBinding(compoundName, type, ProblemReasons.NotVisible);
	return type;
}
MethodBinding findStaticMethod(ReferenceBinding currentType, char[] selector) {
	if (!currentType.canBeSeenBy(this))
		return null;

	do {
		MethodBinding[] methods = currentType.getMethods(selector);
		if (methods != Binding.NO_METHODS) {
			for (int i = methods.length; --i >= 0;) {
				MethodBinding method = methods[i];
				if (method.isStatic() && method.canBeSeenBy(environment.defaultPackage))
					return method;
			}
		}
		if (currentType.superInterfaces() == null) // needed for statically imported types which don't know their hierarchy yet
			((SourceTypeBinding) currentType).classScope.connectTypeHierarchy();
	} while ((currentType = currentType.superclass()) != null);
	return null;
}
ImportBinding[] getDefaultImports() {
	// initialize the default imports if necessary... share the default java.lang.* import
	if (environment.defaultImports != null) return environment.defaultImports;
 
	Binding importBinding = environment.defaultPackage;
//	if (importBinding != null)
//		importBinding = ((PackageBinding) importBinding).getTypeOrPackage(JAVA_LANG[1]);

	// abort if java.lang cannot be found...
	if (importBinding == null || !importBinding.isValidBinding()) {
	// create a proxy for the missing BinaryType
		BinaryTypeBinding missingObject = environment.cacheMissingBinaryType(JAVA_LANG_OBJECT, this.referenceContext);
		importBinding = missingObject.fPackage;
	}
	return environment.defaultImports = new ImportBinding[] {new ImportBinding(new char[][] {SystemLibraryLocation.SYSTEM_LIBARAY_NAME}, true, importBinding, (ImportReference)null)};
}
// NOT Public API
public final Binding getImport(char[][] compoundName, boolean onDemand, boolean isStaticImport) {
	if (onDemand)
		return findImport(compoundName, compoundName.length);
	return findSingleImport(compoundName, isStaticImport);
}

public int nextCaptureID() {
	return this.captureID++;
}

/* Answer the problem reporter to use for raising new problems.
*
* Note that as a side-effect, this updates the current reference context
* (unit, type or method) in case the problem handler decides it is necessary
* to abort.
*/

public ProblemReporter problemReporter() {
	ProblemReporter problemReporter = referenceContext.problemReporter;
	problemReporter.referenceContext = referenceContext;
	return problemReporter;
}

/*
What do we hold onto:

1. when we resolve 'a.b.c', say we keep only 'a.b.c'
 & when we fail to resolve 'c' in 'a.b', lets keep 'a.b.c'
THEN when we come across a new/changed/removed item named 'a.b.c',
 we would find all references to 'a.b.c'
-> This approach fails because every type is resolved in every onDemand import to
 detect collision cases... so the references could be 10 times bigger than necessary.

2. when we resolve 'a.b.c', lets keep 'a.b' & 'c'
 & when we fail to resolve 'c' in 'a.b', lets keep 'a.b' & 'c'
THEN when we come across a new/changed/removed item named 'a.b.c',
 we would find all references to 'a.b' & 'c'
-> This approach does not have a space problem but fails to handle collision cases.
 What happens if a type is added named 'a.b'? We would search for 'a' & 'b' but
 would not find a match.

3. when we resolve 'a.b.c', lets keep 'a', 'a.b' & 'a', 'b', 'c'
 & when we fail to resolve 'c' in 'a.b', lets keep 'a', 'a.b' & 'a', 'b', 'c'
THEN when we come across a new/changed/removed item named 'a.b.c',
 we would find all references to 'a.b' & 'c'
OR 'a.b' -> 'a' & 'b'
OR 'a' -> '' & 'a'
-> As long as each single char[] is interned, we should not have a space problem
 and can handle collision cases.

4. when we resolve 'a.b.c', lets keep 'a.b' & 'a', 'b', 'c'
 & when we fail to resolve 'c' in 'a.b', lets keep 'a.b' & 'a', 'b', 'c'
THEN when we come across a new/changed/removed item named 'a.b.c',
 we would find all references to 'a.b' & 'c'
OR 'a.b' -> 'a' & 'b' in the simple name collection
OR 'a' -> 'a' in the simple name collection
-> As long as each single char[] is interned, we should not have a space problem
 and can handle collision cases.
*/
void recordQualifiedReference(char[][] qualifiedName) {
	if (qualifiedReferences == null) return; // not recording dependencies

	int length = qualifiedName.length;
	if (length > 1) {
		while (!qualifiedReferences.contains(qualifiedName)) {
			qualifiedReferences.add(qualifiedName);
			if (length == 2) {
				recordSimpleReference(qualifiedName[0]);
				recordSimpleReference(qualifiedName[1]);
				return;
			}
			length--;
			recordSimpleReference(qualifiedName[length]);
			System.arraycopy(qualifiedName, 0, qualifiedName = new char[length][], 0, length);
		}
	} else if (length == 1) {
		recordSimpleReference(qualifiedName[0]);
	}
}
void recordReference(char[][] qualifiedEnclosingName, char[] simpleName) {
	recordQualifiedReference(qualifiedEnclosingName);
	recordSimpleReference(simpleName);
}
void recordReference(ReferenceBinding type, char[] simpleName) {
	ReferenceBinding actualType = typeToRecord(type);
	if (actualType != null)
		recordReference(actualType.compoundName, simpleName);
}
void recordSimpleReference(char[] simpleName) {
	if (simpleNameReferences == null) return; // not recording dependencies

	if (!simpleNameReferences.contains(simpleName))
		simpleNameReferences.add(simpleName);
}
void recordSuperTypeReference(TypeBinding type) {
	if (referencedSuperTypes == null) return; // not recording dependencies

	ReferenceBinding actualType = typeToRecord(type);
	if (actualType != null && !referencedSuperTypes.containsIdentical(actualType))
		referencedSuperTypes.add(actualType);
}
public void recordTypeConversion(TypeBinding superType, TypeBinding subType) {
	recordSuperTypeReference(subType); // must record the hierarchy of the subType that is converted to the superType
}
void recordTypeReference(TypeBinding type) {
	if (referencedTypes == null) return; // not recording dependencies

	ReferenceBinding actualType = typeToRecord(type);
	if (actualType != null && !referencedTypes.containsIdentical(actualType))
		referencedTypes.add(actualType);
}
void recordTypeReferences(TypeBinding[] types) {
	if (referencedTypes == null) return; // not recording dependencies
	if (types == null || types.length == 0) return;

	for (int i = 0, max = types.length; i < max; i++) {
		// No need to record supertypes of method arguments & thrown exceptions, just the compoundName
		// If a field/method is retrieved from such a type then a separate call does the job
		ReferenceBinding actualType = typeToRecord(types[i]);
		if (actualType != null && !referencedTypes.containsIdentical(actualType))
			referencedTypes.add(actualType);
	}
}
Binding resolveSingleImport(ImportBinding importBinding) {
	if (importBinding.resolvedImport == null) {
		importBinding.resolvedImport = findSingleImport(importBinding.compoundName, importBinding.isStatic());
		if (!importBinding.resolvedImport.isValidBinding() || importBinding.resolvedImport instanceof PackageBinding) {
			if (this.imports != null) {
				ImportBinding[] newImports = new ImportBinding[imports.length - 1];
				for (int i = 0, n = 0, max = this.imports.length; i < max; i++)
					if (this.imports[i] != importBinding)
						newImports[n++] = this.imports[i];
				this.imports = newImports;
			}
			return null;
		}
	}
	return importBinding.resolvedImport;
}
public void storeDependencyInfo() {
	// add the type hierarchy of each referenced supertype
	// cannot do early since the hierarchy may not be fully resolved
	for (int i = 0; i < referencedSuperTypes.size; i++) { // grows as more types are added
		ReferenceBinding type = (ReferenceBinding) referencedSuperTypes.elementAt(i);
		if (!referencedTypes.containsIdentical(type))
			referencedTypes.add(type);

		if (!type.isLocalType()) {
			ReferenceBinding enclosing = type.enclosingType();
			if (enclosing != null)
				recordSuperTypeReference(enclosing);
		}
		ReferenceBinding superclass = type.superclass();
		if (superclass != null)
			recordSuperTypeReference(superclass);
		ReferenceBinding[] interfaces = type.superInterfaces();
		if (interfaces != null)
			for (int j = 0, length = interfaces.length; j < length; j++)
				recordSuperTypeReference(interfaces[j]);
	}

	for (int i = 0, l = referencedTypes.size; i < l; i++) {
		ReferenceBinding type = (ReferenceBinding) referencedTypes.elementAt(i);
		if (!type.isLocalType())
			recordQualifiedReference(type.isMemberType()
				? CharOperation.splitOn('.', type.readableName())
				: type.compoundName);
	}

	int size = qualifiedReferences.size;
	char[][][] qualifiedRefs = new char[size][][];
	for (int i = 0; i < size; i++)
		qualifiedRefs[i] = qualifiedReferences.elementAt(i);
	referenceContext.compilationResult.qualifiedReferences = qualifiedRefs;

	size = simpleNameReferences.size;
	char[][] simpleRefs = new char[size][];
	for (int i = 0; i < size; i++)
		simpleRefs[i] = simpleNameReferences.elementAt(i);
	referenceContext.compilationResult.simpleNameReferences = simpleRefs;
}
public String toString() {
	return "--- CompilationUnit Scope : " + new String(referenceContext.getFileName()); //$NON-NLS-1$
}
private ReferenceBinding typeToRecord(TypeBinding type) {
	if (type.isArrayType())
		type = ((ArrayBinding) type).leafComponentType;

	switch (type.kind()) {
		case Binding.BASE_TYPE :
		case Binding.TYPE_PARAMETER :
		case Binding.WILDCARD_TYPE :
			return null;
		case Binding.PARAMETERIZED_TYPE :
		case Binding.RAW_TYPE :
			type = type.erasure();
	}
	if (type instanceof CompilationUnitBinding)
		return null;
	ReferenceBinding refType = (ReferenceBinding) type;
	if (refType.isLocalType()) return null;
	return refType;
}
public void verifyMethods(MethodVerifier verifier) {
	for (int i = 0, length = topLevelTypes.length; i < length; i++)
		topLevelTypes[i].verifyMethods(verifier);
 }

public void cleanup()
{

	if (this.referencedTypes!=null)
	  for (int i = 0, l = referencedTypes.size; i < l; i++) {
		Object obj=referencedTypes.elementAt(i);
		if (obj instanceof SourceTypeBinding)
		{
			SourceTypeBinding type = (SourceTypeBinding) obj;
			type.classScope=null;
			type.scope=null;
		}
	}
}
}
