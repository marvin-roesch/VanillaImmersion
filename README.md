<img align="center" src="http://patches.mineformers.de/minecraft/vimmersion/github_images/banner.png?t=0" alt="VanillaImersion Logo">
<div align="center">
  <a href="https://travis-ci.org/PaleoCrafter/VanillaImmersion"><img src="http://services.mineformers.de/travis/build-status/PaleoCrafter/VanillaImmersion" alt="Build Status"></a>
  <a href="http://minecraft.curseforge.com/projects/vanilla-immersion"><img src="http://services.mineformers.de/static/curseforge" alt="CurseForge"></a>
  <a href="https://github.com/PaleoCrafter/VanillaImmersion/issues"><img src="http://services.mineformers.de/static/issues" alt="Issues"></a>
</div>

### Development Setup and Building
This mod follows the standard structure for Minecraft Mods. If you want to work with the code yourself and even build it, follow this guide. Please be aware that basic understanding of programming terms and Java/Kotlin is expected.
To get a development environment running, follow these simple steps:

  1. [Clone the repository](https://help.github.com/articles/cloning-a-repository/) (forking it beforehand is optional, see [Contributing](#contributing))
  2. Open a terminal/command line and browse to the repository's folder
  3. Run `gradlew setupDecompWorkspace`
    - If you are familiar with ForgeGradle, you may of course run one of the other setup tasks
    - This will most definitely take a while, so don't worry if it isn't finished within a couple of seconds
  4. Import the project into the IDE of your choice
    - This step differs depending on the software you are using. See down below for a detailed description of how to import the project in your favourite IDE
  5. You are now ready to edit and run the code

Building the mod is just as easy, simply run `gradlew build` on the command line to initiate it. You will then be able to find the compiled JAR file in `<project directory>/build/libs`. However, there are a few peculiarities of this mod you need to keep in mind before blindly running the command. To prevent sensitive or non-standard data from being distributed, the building process reads a file called `private.properties`, which is excluded through the `.gitignore` settings, to get certain configuration options. To fully benefit from all features of the buildscript, you should create this file yourself. It may contain the following properties:

|Property|Description|
|--------|-----------|
|`cf_api`|A CurseForge API token, used for automatically uploading artifacts to the site. If you want to use the `curseforge` Gradle task, you should get yourself an API token. Information on how to acquire one can be found [here](https://github.com/curseforge/api#generate-a-token).|
|`cf_project`|The ID of a project on CurseForge. This will be used as target by the `curseforge` Gradle task.|
|`cf_type`|The release type to be used for the upload on CurseForge.<br>May be one of the following three: `release`, `beta` or `alpha`|
|`update_url`|An URL pointing to a JSON file which contains information about various versions of the mod. Will be used by MinecraftForge to check the mod for updates. See [this gist](https://gist.github.com/LexManos/7aacb9aa991330523884) for the expected format of the file.|

### Contributing
#### Through Code
If you want to contribute to this mod, especially by comitting Pull Requests for the code base, feel free to do so! The procedure works like anywhere else, but here is a step-by-step guide anyways:

  1. [Fork the repository](https://help.github.com/articles/fork-a-repo/)
  2. [Setup your development environment](https://github.com/PaleoCrafter/VanillaImmersion/new/master?readme=1#development-setup-and-building)
  3. Perform any edits to the code base
  4. Thoroughly test your code
  5. Push and Commit your changes to your fork (preferably to a separate branch)
  6. [Submit the Pull Request](https://help.github.com/articles/creating-a-pull-request/)

Apart from this, there are a few things to keep in mind while working on the code.
First of all, to improve readability, you should stick to a consistent formatting and preferably automatically format the files you edit. For this purpose, we provide a [settings file](https://gist.github.com/PaleoCrafter/abb5a3b62dee0760bf8476191ce27d9d) for IntelliJ IDEA that allows you to just run the built-in formatter without worrying about a mismatched configuration. See [this guide](https://www.jetbrains.com/help/idea/2016.1/copying-code-style-settings.html) for instructions on how to import the file into your IDE. Unfortunately, we can't provide a similar file for eclipse or other IDEs right now, but this will hopefully change in the future.

Secondly, you must be aware that this project strictly uses Kotlin, an alternative language for the JVM. It provides a lot of helpful features that ease the daily development routine, so don't expect this project to change to Java anytime soon. Refrain from contributing to this project if you don't have any experience with Kotlin or programming in general. Instead, maybe [another form](#through-other-means) of contribution is more suitable for you.

Finally, it should also be noted that PRs should be meaningful and at least of a certain extent. Simple contributions like typo fixes or formatting changes will not be accepted since they clutter the commit history and often only are ways of getting mentioned somewhere. Creating an issue about the problem is a much better way of dealing with this since the author can fix a lot of them in bulk later one. The only exception to this is if you yourself fix a batch of typos and submit a PR for it rather than selectively fixing single issues.

#### Through other means
Contribution does not only have to happen through code. Quite the opposite, actually! Any form of participating in the community is greatly appreciated and may reach from helping resolve issues to providing gorgeous artwork. The following is a list of ways to contribute to this mod, expect it to grow in the future:

  - **Reporting issues:** This is probably one of the easiest but also one of the most important ways of contribution. Whenever you encounter a bug, don't shy away from reporting it on the [issue tracker](https://github.com/PaleoCrafter/VanillaImmersion/issues). However, please make sure that your issue is not a duplicate of an existing one beforehand. Also, please do not report issues known to be resolved in newer versions of the mod.
  - **Making suggestions:** If you have a great idea you definitely want to see implemented in this mod, feel free to share it! Simply create an issue on the [tracker](https://github.com/PaleoCrafter/VanillaImmersion/issues) and outline your idea clearly, if possible with images explaining confusing concepts. You may prefix the issue to mark it as a suggestion, this should generally not be necessary though since a "Suggestion" label can be added quickly.
  - **Providing localisations:** Minecraft players come from a large variety of countries and they of course speak many different languages. This mod may not require much attention in this regard since it mostly reuses things from Vanilla, but we're glad about any help with opening the experience to a bigger audience. To perform such a contribution, get familiar with Minecraft's [localisation system](http://minecraft.gamepedia.com/Language) and submit a PR like outlined in the [Code](#through-code) section.
  - **Creating Assets:** If there is one thing programmers are happy about, it is beautiful assets. Again, this mod may not require many textures or models, but any contribution in this category is welcome. Simply submit a Pull Request like shown above or create an [issue](https://github.com/PaleoCrafter/VanillaImmersion/issues) about it if you want feedback on your ideas but don't have any finished asset yet.