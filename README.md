# Test Driven Generator

## TODO
- Implement LangChain tool
- Implement Crew.ai
- Test Code-llama LLM coding
- Test Claude LLM for coding


--------
// TODO: Update this legacy definition 

The idea behind this tool is explained in
this [presentation](https://docs.google.com/presentation/d/1UQeBTu0jupNiNV-Ul943Qz2A4fEjqDScZay45bO6q9A/edit?usp=sharing).

Currently, it only supports JUnit 5 test classes as input.

## Schema

The entire idea in a nutshell.

![schema](/resources/schema_light.png#gh-light-mode-only)
![schema](/resources/schema_dark.png#gh-dark-mode-only)

## Example usage

test-driven-generator.jar <path to JUnit 5 test class>

## Command line arguments

If there is only one command line argument it is assumed that this is a path to a JUnit 5 test class. If there are more
arguments you should use the flags below.

### Possible flags

| flag                | argument                  | description                                                         |
|---------------------|---------------------------|---------------------------------------------------------------------| 
| --test-file         | path to test file         |                                                                     |
| --family            | LLM family                | possible values: ollama, chatgpt                                    |
| --model             | LLM model                 | examples: llama3, gpt-4  (valid models depend on the chosen family) |
| --working-directory | path to working directory |                                                                     |