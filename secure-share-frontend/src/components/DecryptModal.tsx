import React, { useState } from "react";
import { Dialog, DialogTrigger, DialogContent, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface DecryptModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (passphrase: string) => void;
}

const DecryptModal: React.FC<DecryptModalProps> = ({ open, onClose, onConfirm }) => {
  const [passphrase, setPassphrase] = useState("");

  const handleConfirm = () => {
    if (passphrase.trim()) {
      onConfirm(passphrase);
      setPassphrase("");
      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogTitle>Enter Decryption Passphrase</DialogTitle>
        <Input
          type="password"
          placeholder="Passphrase"
          value={passphrase}
          onChange={(e) => setPassphrase(e.target.value)}
        />
        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={handleConfirm}>Decrypt</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default DecryptModal;
