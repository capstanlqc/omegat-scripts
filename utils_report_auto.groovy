//:name=Utils - Calculate auto and enforce :description=

prop = project.projectProperties
if (!prop) {
	console.println("No project open")
	return
}
auto = []
enforced = []

project.allEntries.each { ste ->
    info = project.getTranslationInfo(ste)
    linked = info.linked.toString()
    if (linked == "xAUTO") {
    	auto.add(ste)
    }
    if (linked == "xENFORCED") {
    	enforced.add(ste)
    }
}
console.println("Translated from /tm/auto: ${auto.size()} segments")
console.println("Translated from /tm/enforce: ${enforced.size()} segments")
