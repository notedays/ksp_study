package com.jonesoft.study.processor

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.Variance.*
import com.jonesoft.study.annotations.AutoToString
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStream

class AutoToStringProcessor(
	private val options: Map<String, String>,
	private val logger: KSPLogger,
	private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

	operator fun OutputStream.plusAssign(str: String) {
		this.write(str.toByteArray())
	}

	override fun process(resolver: Resolver): List<KSAnnotated> {
		val packetDataGroup = resolver
			.getSymbolsWithAnnotation(AutoToString::class.java.canonicalName)
			.filterIsInstance<KSClassDeclaration>()

		val fails = mutableListOf<KSAnnotated>()
		packetDataGroup.forEach {
			if (!validate(it, resolver)) {
				fails.add(it)
				return@forEach
			}

			generateCode(it, resolver)
		}

		return emptyList()
	}

	@OptIn(KspExperimental::class)
	private fun validate(classDeclaration: KSClassDeclaration, resolver: Resolver): Boolean {        /*
		 val annotation = classDeclaration.getAnnotationsByType(AutoToString::class).firstOrNull()
			?: let {
				logger.error("Validate Class Error: ${classDeclaration.simpleName}")
				return false
			}
		val includeSuper = annotation.includeSuper
		val superType = classDeclaration.superTypes.first().resolve().declaration as KSClassDeclaration

		if (includeSuper
			&& superType.classKind != ClassKind.INTERFACE
			&& !validate(superType, resolver)
		) return false
		*/

		if (classDeclaration.classKind != ClassKind.CLASS) {
			logger.error("Only classes can be annotated with @${AutoToString::class.java.simpleName}")
			return false
		}

		if (classDeclaration.isAbstract()) {
			logger.error("The class ${classDeclaration.qualifiedName?.asString()} is abstract. You can't annotate abstract classes with @%s")
			return false
		}

		if (!classDeclaration.isPublic()) {
			logger.error("The class %${classDeclaration.qualifiedName?.toString()} is not public.")
			return false
		}

		if (classDeclaration.getConstructors().none { constructor -> constructor.parameters.all { it.hasDefault } }) {
			logger.error("The class ${classDeclaration.qualifiedName?.asString()} should have default constructor.")
			return false
		}

		return true
	}

	@OptIn(KspExperimental::class)
	private fun generateCode(classDeclaration: KSClassDeclaration, resolver: Resolver) {
		val classType = classDeclaration.asType(listOf())
		val classTypeName = classDeclaration.toClassName()

		val parameters = classDeclaration.primaryConstructor!!.parameters
			.filter { !it.isAnnotationPresent(Transient::class) }

		try {
			val constructor = FunSpec.constructorBuilder()
				.apply {
					parameters.map { parameter ->
						val paramName = parameter.name!!.asString()
						val paramType = parameter.type.toTypeName()

						ParameterSpec.builder(paramName, paramType)
							.apply {
								if (parameter.isAnnotationPresent(Transient::class)) {
									addAnnotation(Transient::class)
								}
							}
							.build()
					}.forEach { this.parameters.add(it) }

					callSuperConstructor(*this.parameters.map { it.name }.toTypedArray())
				}
				.build()

			val funAutoToString = FunSpec.builder("toString")
				.addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
				.addCode("return ")
				.addStatement("\"\"\"")
				.addStatement(classDeclaration.toClassName().simpleName)
				.apply {
					parameters.forEach { prop ->
						val propName = prop.name!!.asString()
						this.addStatement("\t- \"$propName\": $$propName".format(propName, propName))
					}
				}
				.addStatement("\"\"\".trimIndent()")
				.returns(String::class)
				.build()

			val sourceClassName = "${classDeclaration.toClassName().simpleName}Impl"
			val classAutoToString =
				TypeSpec.classBuilder(sourceClassName)
					.superclass(classTypeName)
					.addFunction(constructor)
					.addFunction(funAutoToString)
					.build()

			FileSpec.builder(classDeclaration.packageName.asString(), sourceClassName)
				.addType(classAutoToString)
				.build()
				.writeTo(codeGenerator, Dependencies(false, classDeclaration.containingFile!!))
		} catch (e: Exception) {
			logger.error(
				"error in AutoToStringProcessor. message: ${e.localizedMessage}, classType: $classTypeName",
				classType.declaration
			)
		}
	}
}