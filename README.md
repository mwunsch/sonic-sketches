# sonic-sketches

Studies in Overtone and Clojure (and CIDER and Lein).

From that Wikipedia:

> Studies are often used to understand the problems involved in
> rendering subjects and to plan the elements to be used in finished
> works, such as light, color, form, perspective and composition.

This is a repository to house my attempts to better understand the
problems involved in:

+ Overtone, and programming music in general
+ Clojure, its libraries and idioms
+ CIDER, and being productive in Clojure from within Emacs
+ Leiningen, and the ecosystem around Clojure on the JVM
+ core.async, and Communicating Sequential Processes

The final form of this project is the [@sonic_sketches][1] Twitter
bot. Each day at 9 am NYC time, the program is run. The program takes
weather data from the [Forecast API][2] and uses it and a Random
Number Generator to generate a song, which is then uploaded to
Twitter.

Along with the above stated goals, the project is also an exercise in
productionizing the above process with:

+ AWS CodeDeploy for Continuous Delivery
+ AWS CloudFormation for automating the infrastructure
+ Vagrant for creating a reproducible development environment
+ The AWS ecosystem, generally
+ FFmpeg for handling multimedia files

[1]: https://twitter.com/sonic_sketches

[2]: https://developer.forecast.io
