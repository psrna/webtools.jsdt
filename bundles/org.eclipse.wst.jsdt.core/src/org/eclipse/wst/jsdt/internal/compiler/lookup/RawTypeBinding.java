/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.compiler.lookup;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;

/**
 * Denote a raw type, i.e. a generic type referenced without any type arguments.
 * e.g. X<T extends Exception> can be used a raw type 'X', in which case it
 * 	will behave as X<Exception>
 */
public class RawTypeBinding extends ParameterizedTypeBinding {
    
    /**
     * Raw type arguments are erasure of respective parameter bounds. But we may not have resolved
     * these bounds yet if creating raw types while supertype hierarchies are being connected.
     * Therefore, use 'null' instead, and access these in a lazy way later on (when substituting).
     */
	public RawTypeBinding(ReferenceBinding type, ReferenceBinding enclosingType, LookupEnvironment environment){
		super(type, null, enclosingType, environment);
		if (enclosingType == null || (enclosingType.modifiers & ExtraCompilerModifiers.AccGenericSignature) == 0)
			this.modifiers &= ~ExtraCompilerModifiers.AccGenericSignature; // only need signature if enclosing needs one
	}    
	
	public char[] computeUniqueKey(boolean isLeaf) {
	    StringBuffer sig = new StringBuffer(10);
		if (isMemberType() && enclosingType().isParameterizedType()) {
		    char[] typeSig = enclosingType().computeUniqueKey(false/*not a leaf*/);
		    for (int i = 0; i < typeSig.length-1; i++) sig.append(typeSig[i]); // copy all but trailing semicolon
		    sig.append('.').append(sourceName()).append('<').append('>').append(';');
		} else {
		     sig.append(genericType().computeUniqueKey(false/*not a leaf*/));
		     sig.insert(sig.length()-1, "<>"); //$NON-NLS-1$
		}

		int sigLength = sig.length();
		char[] uniqueKey = new char[sigLength];
		sig.getChars(0, sigLength, uniqueKey, 0);						    
		return uniqueKey;
   	}
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.ParameterizedTypeBinding#createParameterizedMethod(org.eclipse.wst.jsdt.internal.compiler.lookup.MethodBinding)
	 */
	public ParameterizedMethodBinding createParameterizedMethod(MethodBinding originalMethod) {
		if (originalMethod.typeVariables == Binding.NO_TYPE_VARIABLES || originalMethod.isStatic()) {
			return super.createParameterizedMethod(originalMethod);
		}
		return this.environment.createParameterizedGenericMethod(originalMethod, this);
	}
	
	public int kind() {
		return RAW_TYPE;
	}	
	
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding#debugName()
	 */
	public String debugName() {
	    StringBuffer nameBuffer = new StringBuffer(10);
		nameBuffer.append(actualType().sourceName()).append("#RAW"); //$NON-NLS-1$
	    return nameBuffer.toString();		
	}	

	/**
	 * Ltype<param1 ... paramN>;
	 * LY<TT;>;
	 */
	public char[] genericTypeSignature() {

	    if (this.genericTypeSignature == null) {
		    StringBuffer sig = new StringBuffer(10);
			if (this.isMemberType() && this.enclosingType().isParameterizedType()) {
			    char[] typeSig = this.enclosingType().genericTypeSignature();
			    for (int i = 0; i < typeSig.length-1; i++) sig.append(typeSig[i]); // copy all but trailing semicolon
			    sig.append('.').append(this.sourceName()).append(';');
				int sigLength = sig.length();
				this.genericTypeSignature = new char[sigLength];
				sig.getChars(0, sigLength, this.genericTypeSignature, 0);						    
			} else {
			     this.genericTypeSignature = genericType().signature(); // erasure
			}
	    }
	   return this.genericTypeSignature;
	}		
	
    public boolean isEquivalentTo(TypeBinding otherType) {
		if (this == otherType) 
		    return true;
	    if (otherType == null) 
	        return false;
	    switch(otherType.kind()) {
	
	    	case Binding.WILDCARD_TYPE :
	        	return ((WildcardBinding) otherType).boundCheck(this);
	    		
	    	case Binding.GENERIC_TYPE :
	    	case Binding.PARAMETERIZED_TYPE :
	    	case Binding.RAW_TYPE :
	            return erasure() == otherType.erasure();
	    }
        return false;
	}
    
    public boolean isIntersectingWith(TypeBinding otherType) {
		if (this == otherType) 
		    return true;
	    if (otherType == null) 
	        return false;
	    switch(otherType.kind()) {
	
	    	case Binding.GENERIC_TYPE :
	    	case Binding.PARAMETERIZED_TYPE :
	    	case Binding.RAW_TYPE :
	            return erasure() == otherType.erasure();
	    }
        return false;
	}
    
	/**
	 * Raw type is not treated as a standard parameterized type
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding#isParameterizedType()
	 */
	public boolean isParameterizedType() {
	    return false;
	}	
	public boolean isRawType() {
	    return true;
	}	
	
	protected void initializeArguments() {
		TypeVariableBinding[] typeVariables = genericType().typeVariables();
		int length = typeVariables.length;
		TypeBinding[] typeArguments = new TypeBinding[length];
		for (int i = 0; i < length; i++) {
			// perform raw conversion on variable upper bound - could cause infinite regression if arguments were initialized lazily
		    typeArguments[i] = this.environment.convertToRawType(typeVariables[i].erasure());
		}
		this.arguments = typeArguments;
	}
	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Binding#readableName()
	 */
	public char[] readableName() /*java.lang.Object,  p.X<T> */ {
	    char[] readableName;
		if (isMemberType()) {
			readableName = CharOperation.concat(enclosingType().readableName(), sourceName, '.');
		} else {
			readableName = CharOperation.concatWith(actualType().compoundName, '.');
		}
		return readableName;
	}

	/**
	 * @see org.eclipse.wst.jsdt.internal.compiler.lookup.Binding#shortReadableName()
	 */
	public char[] shortReadableName() /*Object*/ {
	    char[] shortReadableName;
		if (isMemberType()) {
			shortReadableName = CharOperation.concat(enclosingType().shortReadableName(), sourceName, '.');
		} else {
			shortReadableName = actualType().sourceName;
		}
		return shortReadableName;
	}
}
