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