link: http://localhost:8080/

Link to GitHub repository:
https://github.com/if23b196/Document-Management-System.git

Workflow with Git:
Git Branch Workflow
1. Start from main
Always make sure your main is up to date before starting new work:
git checkout main
git pull origin main

2. Create a feature branch
Give it a clear name (e.g. sprint2, feature-upload, bugfix-login):
git checkout -b sprint2
git push -u origin sprint2
Now you’re working in your own branch, safely away from main.

While working on the feature branch
3. Do your commits
Make changes and commit often with meaningful messages:
git add .
git commit -m "Implement document tagging"

4. Keep your branch up to date
If others are pushing changes to main, bring them into your feature branch regularly to avoid big conflicts later:
git checkout main
git pull origin main
git checkout sprint2
git merge main
# OR, if you prefer clean history:
git rebase main
Merge vs Rebase:
merge → keeps history as-is, adds an extra merge commit (safer for teams).
rebase → rewrites your branch history to look cleaner (better for personal branches).

Finishing your work
5. Merge your feature branch into main
When the feature is done:
git checkout main
git pull origin main   # make sure main is up to date
git merge sprint2
git push origin main
Now your changes are part of main.

Cleaning up branches
6. Delete the feature branch locally and remotely
After merging:
git branch -d sprint2          # delete locally (safe, refuses if unmerged)
git push origin --delete sprint2   # delete remote branch

How do add a project with pom.xml file as a module:
Open Project Structure: Go to File → Project Structure (or press Ctrl+Alt+Shift+S on Windows/Linux or ⌘ + ; on macOS).

Navigate to Modules: In the left sidebar, select Modules.

Add New Module: Click the + (Add) button above the modules list.

Select Import Module: Choose the option Import Module (or New Module → Import Module from existing sources depending on 
your IntelliJ version).

Locate the pom.xml: Navigate to your new worker-service folder and select its pom.xml file.

Confirm: Click OK or Next. IntelliJ will recognize the worker-service folder as a separate, self-contained Maven module.

how to add project with no pom.xml file as a module (untested):
Open Project Structure: Go to File → Project Structure (Ctrl+Alt+Shift+S).

Navigate to Modules: Select your main document-service (or the top-level content root).

Add Content Root: Go to the Sources tab and check the list of content roots. If the ui folder is already inside your 
root DocumentManagementSystem/ folder, it is likely already part of the content root.

Mark as Resource Root (Optional): You can right-click the ui folder in the Project Explorer and mark it as a "Resource 
Root" or "Sources Root" depending on what you're doing with it, but for simple static files, just having it visible is 
often enough.