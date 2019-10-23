package com.github.radium226.config

trait AllSyntax extends ToPartialSyntax
                   with ToCompleteSyntax
                   with MakeOptionSyntax
                   with MakeSubcommandSyntax

trait AllInstances extends ToPartialInstances
                      with ToCompleteInstances
                      with MonoidInstances
                      with ConfigInstances
                      with ConfigReaderInstances
                      with MakeOptionInstances
                      with MakeSubcommandInstances
