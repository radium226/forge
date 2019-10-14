package com.github.radium226.config

trait AllSyntax extends ToPartialSyntax
                   with ToCompleteSyntax

trait AllInstances extends ToPartialInstances
                      with ToCompleteInstances
                      with MonoidInstances
                      with ConfigInstances
                      with ConfigReaderInstances
