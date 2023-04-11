package com.jonesoft.study.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class AutoToStringProcessorProvider : SymbolProcessorProvider {

	override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = AutoToStringProcessor(
		options = environment.options,
		logger = environment.logger,
		codeGenerator = environment.codeGenerator,
	)
	
}